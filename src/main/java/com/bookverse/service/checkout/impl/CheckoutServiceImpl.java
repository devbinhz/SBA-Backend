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
import com.bookverse.dto.request.checkout.GuestCartItemDTO;
import com.bookverse.dto.request.checkout.GuestCheckoutRequestDTO;
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
import com.bookverse.repository.UserVoucherRepository;
import com.bookverse.service.checkout.CheckoutService;
import com.bookverse.entity.UserVoucher;
import com.bookverse.enums.VoucherStatus;
import com.bookverse.enums.DiscountType;
import com.bookverse.enums.DeliveryType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private final UserVoucherRepository userVoucherRepository;
    private final OrderProperties orderProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public CheckoutPreviewResponseDTO preview(Long userId, CheckoutRequestDTO request) {
        if (orderRepository.existsByUserIdAndStatus(userId, OrderStatus.PENDING_PAYMENT)) {
            throw new BadRequestException("You have a pending payment order. Please complete the payment before continuing.");
        }
        User user = getValidUser(userId);
        getOwnedAddress(user.getId(), request.getAddressId());
        List<CartItem> cartItems = getSelectedCartItems(user.getId(), selectedCartItemIds(request));
        List<CheckoutLine> lines = validateAndPrice(cartItems);
        long subtotal = subtotal(lines);
        long discountAmount = calculateDiscount(user.getId(), request.getUserVoucherId(), subtotal);
        return toPreview(lines, discountAmount, request.getDeliveryType());
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutPreviewResponseDTO previewGuest(GuestCheckoutRequestDTO request) {
        List<CheckoutLine> lines = validateAndPriceDTO(request.getItems());
        long subtotal = subtotal(lines);
        return toPreview(lines, 0L, request.getDeliveryType());
    }

    @Override
    @Transactional
    public CheckoutResponseDTO checkout(Long userId, String idempotencyKey, CheckoutRequestDTO request) {
        if (orderRepository.existsByUserIdAndStatus(userId, OrderStatus.PENDING_PAYMENT)) {
            throw new BadRequestException("You have a pending payment order. Please complete the payment before continuing.");
        }
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        User user = getValidUser(userId);

        var existingOrder = orderRepository.findByUserIdAndIdempotencyKey(user.getId(), normalizedKey);
        if (existingOrder.isPresent()) {
            return existingCheckoutResponse(existingOrder.get());
        }

        Address address = getOwnedAddress(user.getId(), request.getAddressId());
        Cart cart = getCart(user.getId());
        Set<Long> selectedCartItemIds = selectedCartItemIds(request);
        List<CartItem> cartItems = getSelectedCartItems(cart, selectedCartItemIds);

        List<CheckoutLine> lines = validateAndPrice(cartItems);
        for (CheckoutLine line : lines) {
            int updated = bookRepository.holdStock(line.book().getId(), line.quantity());
            if (updated != 1) {
                throw new OutOfStockException("Book is out of stock: " + line.book().getTitle());
            }
        }

        long subtotal = subtotal(lines);
        
        UserVoucher userVoucher = null;
        long discountAmount = 0L;
        if (request.getUserVoucherId() != null) {
            userVoucher = validateAndGetUserVoucherForUpdate(user.getId(), request.getUserVoucherId(), subtotal);
            discountAmount = calculateDiscountAmount(userVoucher, subtotal);
            // Mark as used
            userVoucher.setStatus(VoucherStatus.USED);
            userVoucher.setUsedAt(Instant.now());
            userVoucherRepository.save(userVoucher);
        }

        long shippingFee = orderProperties.shippingFeeVnd();
        DeliveryType deliveryType = request.getDeliveryType();
        long giftWrapFee = deliveryType.giftWrapFeeVnd();
        long total = Math.max(0L, subtotal - discountAmount + shippingFee + giftWrapFee);
        Instant expiresAt = Instant.now().plusSeconds(orderProperties.expirationMinutes() * 60);

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING_PAYMENT)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .deliveryType(deliveryType)
                .giftWrapFee(giftWrapFee)
                .discountAmount(discountAmount)
                .userVoucher(userVoucher)
                .total(total)
                .addressSnapshot(addressSnapshot(address))
                .paymentMethod(PaymentProvider.VNPAY)
                .idempotencyKey(normalizedKey)
                .expiresAt(expiresAt)
                .build();

        order = saveOrder(order);

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

        cartItemRepository.deleteByCartIdAndIdIn(cart.getId(), List.copyOf(selectedCartItemIds));

        return toCheckoutResponse(order, payment, orderItems);
    }

    @Override
    @Transactional
    public CheckoutResponseDTO checkoutGuest(String idempotencyKey, GuestCheckoutRequestDTO request) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        // Guest email is optional: "guestEmail = null" never matches in SQL, so retries
        // from anonymous guests need a dedicated null-safe lookup to stay idempotent.
        var existingOrder = request.getEmail() != null
                ? orderRepository.findByGuestEmailAndIdempotencyKey(request.getEmail(), normalizedKey)
                : orderRepository.findByIdempotencyKeyAndUserIsNullAndGuestEmailIsNull(normalizedKey);
        if (existingOrder.isPresent()) {
            return existingCheckoutResponse(existingOrder.get());
        }

        List<CheckoutLine> lines = validateAndPriceDTO(request.getItems());
        for (CheckoutLine line : lines) {
            int updated = bookRepository.holdStock(line.book().getId(), line.quantity());
            if (updated != 1) {
                throw new OutOfStockException("Book is out of stock: " + line.book().getTitle());
            }
        }

        long subtotal = subtotal(lines);
        long shippingFee = orderProperties.shippingFeeVnd();
        DeliveryType deliveryType = request.getDeliveryType();
        long giftWrapFee = deliveryType.giftWrapFeeVnd();
        long total = Math.max(0L, subtotal + shippingFee + giftWrapFee); // No discount for guests
        Instant expiresAt = Instant.now().plusSeconds(orderProperties.expirationMinutes() * 60);

        Order order = Order.builder()
                .user(null)
                .guestEmail(request.getEmail())
                .status(OrderStatus.PENDING_PAYMENT)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .deliveryType(deliveryType)
                .giftWrapFee(giftWrapFee)
                .discountAmount(0L)
                .userVoucher(null)
                .total(total)
                .addressSnapshot(addressSnapshot(request))
                .paymentMethod(PaymentProvider.VNPAY)
                .idempotencyKey(normalizedKey)
                .expiresAt(expiresAt)
                .build();

        order = saveOrder(order);

        List<OrderItem> orderItems = saveOrderItems(order, lines);
        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .provider(PaymentProvider.VNPAY)
                .status(PaymentStatus.PENDING)
                .amount(order.getTotal())
                .providerOrderCode(order.getId() * 1000 + 1)
                .build());

        saveStockMovements(null, order, lines);
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .toStatus(OrderStatus.PENDING_PAYMENT)
                .changedBy(null)
                .note("Guest order created from checkout")
                .build());

        return toCheckoutResponse(order, payment, orderItems);
    }

    private Order saveOrder(Order order) {
        try {
            return orderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException exception) {
            if (isIdempotencyConstraintViolation(exception)) {
                throw new IdempotencyConflictException("Idempotency key is already used");
            }
            throw exception;
        }
    }

    private boolean isIdempotencyConstraintViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && "uk_orders_idempotency_key".equalsIgnoreCase(constraintViolation.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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

    private Cart getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartEmptyException("Cart is empty"));
        return cart;
    }

    private List<CartItem> getSelectedCartItems(Long userId, Set<Long> selectedCartItemIds) {
        return getSelectedCartItems(getCart(userId), selectedCartItemIds);
    }

    private List<CartItem> getSelectedCartItems(Cart cart, Set<Long> selectedCartItemIds) {
        if (selectedCartItemIds.isEmpty()) {
            throw new CartEmptyException("No cart items selected");
        }
        List<CartItem> items = cartItemRepository.findByCartIdAndIdInOrderByIdAsc(
                cart.getId(),
                List.copyOf(selectedCartItemIds)
        );
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("Selected cart items not found");
        }
        if (items.size() != selectedCartItemIds.size()) {
            throw new ResourceNotFoundException("One or more selected cart items were not found in your cart");
        }
        return items;
    }

    private Set<Long> selectedCartItemIds(CheckoutRequestDTO request) {
        if (request.getCartItemIds() == null || request.getCartItemIds().isEmpty()) {
            throw new CartEmptyException("No cart items selected");
        }
        if (request.getCartItemIds().stream().anyMatch(id -> id == null)) {
            throw new BadRequestException("Cart item id is required");
        }
        return new LinkedHashSet<>(request.getCartItemIds());
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

    private List<CheckoutLine> validateAndPriceDTO(List<GuestCartItemDTO> items) {
        return items.stream()
                .map(item -> {
                    Book book = bookRepository.findById(item.getBookId())
                            .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + item.getBookId()));
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

    private String addressSnapshot(GuestCheckoutRequestDTO request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", null);
        snapshot.put("recipient", request.getRecipient());
        snapshot.put("phone", request.getPhone());
        snapshot.put("line", request.getLine());
        snapshot.put("ward", request.getWard());
        snapshot.put("district", request.getDistrict());
        snapshot.put("city", request.getCity());
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
                        .createdBy(user != null ? user.getId() : null)
                        .build())
                .toList();
        stockMovementRepository.saveAll(movements);
    }

    private CheckoutPreviewResponseDTO toPreview(
            List<CheckoutLine> lines,
            long discountAmount,
            DeliveryType deliveryType
    ) {
        long subtotal = subtotal(lines);
        long shippingFee = orderProperties.shippingFeeVnd();
        long giftWrapFee = deliveryType.giftWrapFeeVnd();
        long total = Math.max(0L, subtotal - discountAmount + shippingFee + giftWrapFee);
        return CheckoutPreviewResponseDTO.builder()
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .deliveryType(deliveryType)
                .giftWrapFee(giftWrapFee)
                .discountAmount(discountAmount)
                .total(total)
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
                .deliveryType(order.getDeliveryType())
                .giftWrapFee(order.getGiftWrapFee())
                .discountAmount(order.getDiscountAmount())
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

    private UserVoucher validateAndGetUserVoucher(Long userId, Long userVoucherId, long subtotal) {
        return validateUserVoucher(userVoucherRepository.findByIdAndUserId(userVoucherId, userId), subtotal);
    }

    private UserVoucher validateAndGetUserVoucherForUpdate(Long userId, Long userVoucherId, long subtotal) {
        return validateUserVoucher(userVoucherRepository.findWithLockByIdAndUserId(userVoucherId, userId), subtotal);
    }

    private UserVoucher validateUserVoucher(Optional<UserVoucher> found, long subtotal) {
        UserVoucher userVoucher = found
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found or does not belong to you"));

        if (userVoucher.getStatus() != VoucherStatus.UNUSED) {
            throw new BadRequestException("Voucher has already been used or is expired");
        }
        if (Instant.now().isAfter(userVoucher.getExpiresAt())) {
            throw new BadRequestException("Voucher has expired");
        }
        if (subtotal < userVoucher.getVoucher().getTierMinAmount()) {
            throw new BadRequestException("Order subtotal does not meet the minimum amount for this voucher");
        }
        return userVoucher;
    }

    private long calculateDiscountAmount(UserVoucher userVoucher, long subtotal) {
        if (userVoucher.getVoucher().getDiscountType() == DiscountType.FIXED) {
            return Math.min(subtotal, userVoucher.getVoucher().getDiscountValue());
        } else {
            return Math.min(subtotal, (long) (subtotal * (userVoucher.getVoucher().getDiscountValue() / 100.0)));
        }
    }

    private long calculateDiscount(Long userId, Long userVoucherId, long subtotal) {
        if (userVoucherId == null) {
            return 0L;
        }
        UserVoucher userVoucher = validateAndGetUserVoucher(userId, userVoucherId, subtotal);
        return calculateDiscountAmount(userVoucher, subtotal);
    }

    private record CheckoutLine(Book book, int quantity, Long unitPrice, Long lineTotal) {
    }
}
