package com.bookverse.service.checkout.impl;

import com.bookverse.common.exception.AccountDisabledException;
import com.bookverse.common.exception.BadRequestException;
import com.bookverse.common.exception.BookInactiveException;
import com.bookverse.common.exception.CartEmptyException;
import com.bookverse.common.exception.EmailNotVerifiedException;
import com.bookverse.common.exception.IdempotencyConflictException;
import com.bookverse.common.exception.OutOfStockException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.config.OrderProperties;
import com.bookverse.dto.request.checkout.CheckoutRequestDTO;
import com.bookverse.dto.response.checkout.CheckoutItemResponseDTO;
import com.bookverse.dto.response.checkout.CheckoutPreviewResponseDTO;
import com.bookverse.dto.response.checkout.CheckoutResponseDTO;
import com.bookverse.entity.Address;
import com.bookverse.entity.Book;
import com.bookverse.entity.Cart;
import com.bookverse.entity.CartItem;
import com.bookverse.entity.Order;
import com.bookverse.entity.OrderItem;
import com.bookverse.entity.OrderStatusHistory;
import com.bookverse.entity.Payment;
import com.bookverse.entity.StockMovement;
import com.bookverse.entity.User;
import com.bookverse.enums.OrderStatus;
import com.bookverse.enums.PaymentProvider;
import com.bookverse.enums.PaymentStatus;
import com.bookverse.enums.StockMovementReason;
import com.bookverse.repository.AddressRepository;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.CartItemRepository;
import com.bookverse.repository.CartRepository;
import com.bookverse.repository.OrderItemRepository;
import com.bookverse.repository.OrderRepository;
import com.bookverse.repository.OrderStatusHistoryRepository;
import com.bookverse.repository.PaymentRepository;
import com.bookverse.repository.StockMovementRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.checkout.CheckoutService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderProperties orderProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public CheckoutPreviewResponseDTO preview(Long userId, CheckoutRequestDTO request) {
        User user = getValidUser(userId);
        getOwnedAddress(user.getId(), request.getAddressId());
        List<CartItem> cartItems = getCartItems(user.getId());
        List<CheckoutLine> lines = validateAndPrice(cartItems);
        return toPreview(lines);
    }

    @Override
    @Transactional
    public CheckoutResponseDTO checkout(Long userId, String idempotencyKey, CheckoutRequestDTO request) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        User user = getValidUser(userId);

        var existingOrder = orderRepository.findByUserIdAndIdempotencyKey(user.getId(), normalizedKey);
        if (existingOrder.isPresent()) {
            return existingCheckoutResponse(existingOrder.get());
        }

        Address address = getOwnedAddress(user.getId(), request.getAddressId());
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CartEmptyException("Cart is empty"));
        List<CartItem> cartItems = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        if (cartItems.isEmpty()) {
            throw new CartEmptyException("Cart is empty");
        }

        List<CheckoutLine> lines = validateAndPrice(cartItems);
        for (CheckoutLine line : lines) {
            int updated = bookRepository.holdStock(line.book().getId(), line.quantity());
            if (updated != 1) {
                throw new OutOfStockException("Book is out of stock: " + line.book().getTitle());
            }
        }

        long subtotal = subtotal(lines);
        long shippingFee = orderProperties.shippingFeeVnd();
        Instant expiresAt = Instant.now().plusSeconds(orderProperties.expirationMinutes() * 60);

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING_PAYMENT)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .total(subtotal + shippingFee)
                .addressSnapshot(addressSnapshot(address))
                .paymentMethod(PaymentProvider.VNPAY)
                .idempotencyKey(normalizedKey)
                .expiresAt(expiresAt)
                .build();

        try {
            order = orderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException exception) {
            throw new IdempotencyConflictException("Idempotency key is already used");
        }

        List<OrderItem> orderItems = saveOrderItems(order, lines);
        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .provider(PaymentProvider.VNPAY)
                .status(PaymentStatus.PENDING)
                .amount(order.getTotal())
                .providerOrderCode(order.getId() * 1000 + 1)
                .build());

        saveStockMovements(user, order, lines);
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .toStatus(OrderStatus.PENDING_PAYMENT)
                .changedBy(user)
                .note("Order created from checkout")
                .build());

        cartItemRepository.deleteByCartId(cart.getId());

        return toCheckoutResponse(order, payment, orderItems);
    }

    private User getValidUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isEnabled()) {
            throw new AccountDisabledException("Account is disabled");
        }
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email is not verified");
        }
        return user;
    }

    private Address getOwnedAddress(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    private List<CartItem> getCartItems(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartEmptyException("Cart is empty"));
        List<CartItem> items = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        if (items.isEmpty()) {
            throw new CartEmptyException("Cart is empty");
        }
        return items;
    }

    private List<CheckoutLine> validateAndPrice(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> {
                    Book book = item.getBook();
                    if (!book.isActive() || !book.getCategory().isActive()) {
                        throw new BookInactiveException("Book is inactive: " + book.getTitle());
                    }
                    if (item.getQuantity() <= 0) {
                        throw new BadRequestException("Cart item quantity must be greater than zero");
                    }
                    if (book.getStock() < item.getQuantity()) {
                        throw new OutOfStockException("Book is out of stock: " + book.getTitle());
                    }
                    long lineTotal = book.getPrice() * item.getQuantity();
                    return new CheckoutLine(book, item.getQuantity(), book.getPrice(), lineTotal);
                })
                .toList();
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > 100) {
            throw new BadRequestException("Idempotency-Key must be at most 100 characters");
        }
        return normalized;
    }

    private String addressSnapshot(Address address) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", address.getId());
        snapshot.put("recipient", address.getRecipient());
        snapshot.put("phone", address.getPhone());
        snapshot.put("line", address.getLine());
        snapshot.put("ward", address.getWard());
        snapshot.put("district", address.getDistrict());
        snapshot.put("city", address.getCity());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Address snapshot is invalid");
        }
    }

    private List<OrderItem> saveOrderItems(Order order, List<CheckoutLine> lines) {
        List<OrderItem> items = lines.stream()
                .map(line -> OrderItem.builder()
                        .order(order)
                        .book(line.book())
                        .titleSnapshot(line.book().getTitle())
                        .unitPrice(line.unitPrice())
                        .quantity(line.quantity())
                        .lineTotal(line.lineTotal())
                        .build())
                .toList();
        return orderItemRepository.saveAll(items);
    }

    private void saveStockMovements(User user, Order order, List<CheckoutLine> lines) {
        List<StockMovement> movements = lines.stream()
                .map(line -> StockMovement.builder()
                        .book(line.book())
                        .orderId(order.getId())
                        .delta(-line.quantity())
                        .reason(StockMovementReason.ORDER_HOLD)
                        .operationKey("checkout:" + order.getId() + ":hold:" + line.book().getId())
                        .note("Stock held for checkout")
                        .createdBy(user.getId())
                        .build())
                .toList();
        stockMovementRepository.saveAll(movements);
    }

    private CheckoutPreviewResponseDTO toPreview(List<CheckoutLine> lines) {
        long subtotal = subtotal(lines);
        long shippingFee = orderProperties.shippingFeeVnd();
        return CheckoutPreviewResponseDTO.builder()
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .total(subtotal + shippingFee)
                .items(lines.stream().map(this::toItemResponse).toList())
                .build();
    }

    private CheckoutResponseDTO existingCheckoutResponse(Order order) {
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        return toCheckoutResponse(order, payment, items);
    }

    private CheckoutResponseDTO toCheckoutResponse(Order order, Payment payment, List<OrderItem> items) {
        return CheckoutResponseDTO.builder()
                .orderId(order.getId())
                .paymentId(payment.getId())
                .orderStatus(order.getStatus())
                .paymentStatus(payment.getStatus())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .providerOrderCode(payment.getProviderOrderCode())
                .checkoutUrl(payment.getCheckoutUrl())
                .expiresAt(order.getExpiresAt())
                .items(items.stream().map(this::toItemResponse).toList())
                .build();
    }

    private CheckoutItemResponseDTO toItemResponse(CheckoutLine line) {
        return CheckoutItemResponseDTO.builder()
                .bookId(line.book().getId())
                .title(line.book().getTitle())
                .unitPrice(line.unitPrice())
                .quantity(line.quantity())
                .lineTotal(line.lineTotal())
                .build();
    }

    private CheckoutItemResponseDTO toItemResponse(OrderItem item) {
        return CheckoutItemResponseDTO.builder()
                .bookId(item.getBook().getId())
                .title(item.getTitleSnapshot())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .lineTotal(item.getLineTotal())
                .build();
    }

    private long subtotal(List<CheckoutLine> lines) {
        return lines.stream().mapToLong(CheckoutLine::lineTotal).sum();
    }

    private record CheckoutLine(Book book, int quantity, Long unitPrice, Long lineTotal) {
    }
}
