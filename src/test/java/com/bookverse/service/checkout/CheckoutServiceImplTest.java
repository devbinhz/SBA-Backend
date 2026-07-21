package com.bookverse.service.checkout;

import com.bookverse.common.exception.CartEmptyException;
import com.bookverse.common.exception.BadRequestException;
import com.bookverse.config.OrderProperties;
import com.bookverse.dto.request.checkout.CheckoutRequestDTO;
import com.bookverse.dto.request.checkout.GuestCartItemDTO;
import com.bookverse.dto.request.checkout.GuestCheckoutRequestDTO;
import com.bookverse.entity.Address;
import com.bookverse.entity.Book;
import com.bookverse.entity.Cart;
import com.bookverse.entity.CartItem;
import com.bookverse.entity.Category;
import com.bookverse.entity.GiftWrap;
import com.bookverse.entity.Order;
import com.bookverse.entity.Payment;
import com.bookverse.entity.User;
import com.bookverse.entity.UserVoucher;
import com.bookverse.entity.Voucher;
import com.bookverse.enums.DiscountType;
import com.bookverse.enums.UserVoucherStatus;
import com.bookverse.enums.OrderStatus;
import com.bookverse.enums.PaymentProvider;
import com.bookverse.enums.PaymentStatus;
import com.bookverse.enums.DeliveryType;
import com.bookverse.enums.UserRole;
import com.bookverse.enums.VoucherStatus;
import com.bookverse.enums.UserVoucherStatus;
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
import com.bookverse.service.checkout.impl.CheckoutServiceImpl;
import com.bookverse.service.giftwrap.GiftWrapService;
import com.bookverse.integration.mail.MailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutServiceImplTest {

    private UserRepository userRepository;
    private AddressRepository addressRepository;
    private CartRepository cartRepository;
    private CartItemRepository cartItemRepository;
    private BookRepository bookRepository;
    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private PaymentRepository paymentRepository;
    private StockMovementRepository stockMovementRepository;
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    private UserVoucherRepository userVoucherRepository;
    private OrderProperties orderProperties;
    private MailService mailService;
    private GiftWrapService giftWrapService;
    private CheckoutServiceImpl checkoutService;

    private static final long GIFT_WRAP_ID = 99L;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        addressRepository = mock(AddressRepository.class);
        cartRepository = mock(CartRepository.class);
        cartItemRepository = mock(CartItemRepository.class);
        bookRepository = mock(BookRepository.class);
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        orderStatusHistoryRepository = mock(OrderStatusHistoryRepository.class);
        userVoucherRepository = mock(UserVoucherRepository.class);
        orderProperties = new OrderProperties(30000L, 15);
        mailService = mock(MailService.class);
        giftWrapService = mock(GiftWrapService.class);
        GiftWrap giftWrap = GiftWrap.builder()
                .id(GIFT_WRAP_ID)
                .name("Red Floral Wrap")
                .imageKey("gift-wrap/red-floral.jpg")
                .feeVnd(10_000L)
                .active(true)
                .build();
        when(giftWrapService.getActiveGiftWrapOrThrow(GIFT_WRAP_ID)).thenReturn(giftWrap);
        checkoutService = new CheckoutServiceImpl(
                userRepository,
                addressRepository,
                cartRepository,
                cartItemRepository,
                bookRepository,
                orderRepository,
                orderItemRepository,
                paymentRepository,
                stockMovementRepository,
                orderStatusHistoryRepository,
                userVoucherRepository,
                orderProperties,
                new ObjectMapper(),
                mailService,
                giftWrapService
        );
    }

    @Test
    void previewRecalculatesCurrentPriceAndShippingFee() {
        User user = customer();
        Address address = address(user);
        Cart cart = Cart.builder().id(9L).user(user).build();
        CartItem item = CartItem.builder().id(3L).cart(cart).book(book(250000, 5)).quantity(2).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndIdInOrderByIdAsc(9L, List.of(3L))).thenReturn(List.of(item));

        var response = checkoutService.preview(1L, request());

        assertThat(response.getSubtotal()).isEqualTo(500000);
        assertThat(response.getShippingFee()).isEqualTo(30000);
        assertThat(response.getTotal()).isEqualTo(530000);
        assertThat(response.getDeliveryType()).isEqualTo(DeliveryType.SELF);
        assertThat(response.getGiftWrapFee()).isZero();
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void previewAddsServerCalculatedGiftWrapFee() {
        User user = customer();
        Address address = address(user);
        Cart cart = Cart.builder().id(9L).user(user).build();
        CartItem item = CartItem.builder().id(3L).cart(cart).book(book(250000, 5)).quantity(2).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndIdInOrderByIdAsc(9L, List.of(3L))).thenReturn(List.of(item));
        CheckoutRequestDTO request = request();
        request.setDeliveryType(DeliveryType.GIFT);
        request.setGiftWrapId(GIFT_WRAP_ID);

        var response = checkoutService.preview(1L, request);

        assertThat(response.getDeliveryType()).isEqualTo(DeliveryType.GIFT);
        assertThat(response.getGiftWrapFee()).isEqualTo(10_000L);
        assertThat(response.getGiftWrapId()).isEqualTo(GIFT_WRAP_ID);
        assertThat(response.getGiftWrapName()).isEqualTo("Red Floral Wrap");
        assertThat(response.getTotal()).isEqualTo(540_000L);
    }

    @Test
    void previewRejectsGiftDeliveryWithoutGiftWrapSelection() {
        User user = customer();
        Address address = address(user);
        Cart cart = Cart.builder().id(9L).user(user).build();
        CartItem item = CartItem.builder().id(3L).cart(cart).book(book(250000, 5)).quantity(2).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndIdInOrderByIdAsc(9L, List.of(3L))).thenReturn(List.of(item));
        CheckoutRequestDTO request = request();
        request.setDeliveryType(DeliveryType.GIFT);

        assertThatThrownBy(() -> checkoutService.preview(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Gift wrap selection is required");
    }

    @Test
    void previewAppliesFixedVoucherWithoutConsumingIt() {
        User user = customer();
        Address address = address(user);
        Cart cart = Cart.builder().id(9L).user(user).build();
        CartItem item = CartItem.builder().id(3L).cart(cart).book(book(250_000, 5)).quantity(2).build();
        UserVoucher voucher = userVoucher(user, DiscountType.FIXED_AMOUNT, 20_000L, 200_000L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndIdInOrderByIdAsc(9L, List.of(3L))).thenReturn(List.of(item));
        when(userVoucherRepository.findByIdAndUserId(21L, 1L)).thenReturn(Optional.of(voucher));
        CheckoutRequestDTO request = request();
        request.setUserVoucherId(21L);

        var response = checkoutService.preview(1L, request);

        assertThat(response.getDiscountAmount()).isEqualTo(20_000L);
        assertThat(response.getTotal()).isEqualTo(510_000L);
        assertThat(voucher.getStatus()).isEqualTo(UserVoucherStatus.UNUSED);
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    void previewRejectsExpiredVoucher() {
        User user = customer();
        Address address = address(user);
        Cart cart = Cart.builder().id(9L).user(user).build();
        CartItem item = CartItem.builder().id(3L).cart(cart).book(book(250_000, 5)).quantity(2).build();
        UserVoucher voucher = userVoucher(user, DiscountType.FIXED_AMOUNT, 20_000L, 200_000L);
        voucher.setExpiresAt(Instant.now().minusSeconds(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndIdInOrderByIdAsc(9L, List.of(3L))).thenReturn(List.of(item));
        when(userVoucherRepository.findByIdAndUserId(21L, 1L)).thenReturn(Optional.of(voucher));
        CheckoutRequestDTO request = request();
        request.setUserVoucherId(21L);

        assertThatThrownBy(() -> checkoutService.preview(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void previewRejectsVoucherWhenSubtotalIsBelowMinimum() {
        User user = customer();
        Address address = address(user);
        Cart cart = Cart.builder().id(9L).user(user).build();
        CartItem item = CartItem.builder().id(3L).cart(cart).book(book(100_000, 5)).quantity(1).build();
        UserVoucher voucher = userVoucher(user, DiscountType.FIXED_AMOUNT, 20_000L, 200_000L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndIdInOrderByIdAsc(9L, List.of(3L))).thenReturn(List.of(item));
        when(userVoucherRepository.findByIdAndUserId(21L, 1L)).thenReturn(Optional.of(voucher));
        CheckoutRequestDTO request = request();
        request.setUserVoucherId(21L);

        assertThatThrownBy(() -> checkoutService.preview(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("minimum amount");
    }

    @Test
    void checkoutHoldsStockCreatesPendingPaymentAndClearsSelectedCartItems() {
        User user = customer();
        Address address = address(user);
        Cart cart = Cart.builder().id(9L).user(user).build();
        Book book = book(250000, 5);
        CartItem item = CartItem.builder().id(3L).cart(cart).book(book).quantity(2).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "key-1")).thenReturn(Optional.empty());
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndIdInOrderByIdAsc(9L, List.of(3L))).thenReturn(List.of(item));
        when(bookRepository.holdStock(10L, 2)).thenReturn(1);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1001L);
            return order;
        });
        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(501L);
            return payment;
        });

        CheckoutRequestDTO giftRequest = request();
        giftRequest.setDeliveryType(DeliveryType.GIFT);
        giftRequest.setGiftWrapId(GIFT_WRAP_ID);

        long beforeMillis = System.currentTimeMillis();
        var response = checkoutService.checkout(1L, " key-1 ", giftRequest);
        long afterMillis = System.currentTimeMillis();

        var orderCaptor = forClass(Order.class);
        var paymentCaptor = forClass(Payment.class);
        assertThat(response.getOrderId()).isEqualTo(1001L);
        assertThat(response.getPaymentId()).isEqualTo(501L);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        // providerOrderCode = currentTimeMillis * 100 + (orderId % 100); orderId 1001 -> low digits "01".
        assertThat(response.getProviderOrderCode() % 100).isEqualTo(1L);
        assertThat(response.getProviderOrderCode() / 100).isBetween(beforeMillis, afterMillis);
        assertThat(response.getCheckoutUrl()).isNull();
        verify(orderRepository).saveAndFlush(orderCaptor.capture());
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(orderCaptor.getValue().getAddressSnapshot()).contains("Nguyen Van A", "0900000000", "123 Street", "Ho Chi Minh");
        assertThat(orderCaptor.getValue().getPaymentMethod()).isEqualTo(PaymentProvider.VNPAY);
        assertThat(response.getDeliveryType()).isEqualTo(DeliveryType.GIFT);
        assertThat(response.getGiftWrapFee()).isEqualTo(10_000L);
        assertThat(response.getGiftWrapId()).isEqualTo(GIFT_WRAP_ID);
        assertThat(response.getGiftWrapName()).isEqualTo("Red Floral Wrap");
        assertThat(response.getTotal()).isEqualTo(540_000L);
        assertThat(orderCaptor.getValue().getDeliveryType()).isEqualTo(DeliveryType.GIFT);
        assertThat(orderCaptor.getValue().getGiftWrapFee()).isEqualTo(10_000L);
        assertThat(orderCaptor.getValue().getGiftWrap()).isNotNull();
        assertThat(orderCaptor.getValue().getGiftWrap().getId()).isEqualTo(GIFT_WRAP_ID);
        assertThat(paymentCaptor.getValue().getProvider()).isEqualTo(PaymentProvider.VNPAY);
        assertThat(paymentCaptor.getValue().getAmount()).isEqualTo(540_000L);
        assertThat(paymentCaptor.getValue().getProviderOrderCode()).isEqualTo(response.getProviderOrderCode());
        verify(bookRepository).holdStock(10L, 2);
        verify(stockMovementRepository).saveAll(any());
        verify(orderStatusHistoryRepository).save(any());
        verify(cartItemRepository).deleteByCartIdAndIdIn(9L, List.of(3L));
    }

    @Test
    void checkoutWithCodConfirmsOrderToPaidImmediately() {
        User user = customer();
        Address address = address(user);
        Cart cart = Cart.builder().id(9L).user(user).build();
        Book book = book(250000, 5);
        CartItem item = CartItem.builder().id(3L).cart(cart).book(book).quantity(2).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "cod-key")).thenReturn(Optional.empty());
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndIdInOrderByIdAsc(9L, List.of(3L))).thenReturn(List.of(item));
        when(bookRepository.holdStock(10L, 2)).thenReturn(1);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1003L);
            return order;
        });
        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequestDTO codRequest = request();
        codRequest.setPaymentMethod(PaymentProvider.COD);

        var response = checkoutService.checkout(1L, "cod-key", codRequest);

        var orderCaptor = forClass(Order.class);
        var paymentCaptor = forClass(Payment.class);
        verify(orderRepository).saveAndFlush(orderCaptor.capture());
        verify(paymentRepository).save(paymentCaptor.capture());

        assertThat(response.getPaymentMethod()).isEqualTo(PaymentProvider.COD);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(orderCaptor.getValue().getPaymentMethod()).isEqualTo(PaymentProvider.COD);
        assertThat(paymentCaptor.getValue().getProvider()).isEqualTo(PaymentProvider.COD);
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        // Order mutated in-place to PAID after the initial PENDING_PAYMENT save/flush.
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(orderCaptor.getValue().getPaidAt()).isNotNull();
        assertThat(orderCaptor.getValue().getExpiresAt()).isNull();
        verify(orderStatusHistoryRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void checkoutAppliesPercentageVoucherAndMarksItUsed() {
        User user = customer();
        Address address = address(user);
        Cart cart = Cart.builder().id(9L).user(user).build();
        Book book = book(250_000, 5);
        CartItem item = CartItem.builder().id(3L).cart(cart).book(book).quantity(2).build();
        UserVoucher voucher = userVoucher(user, DiscountType.PERCENTAGE, 10L, 300_000L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "voucher-key")).thenReturn(Optional.empty());
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndIdInOrderByIdAsc(9L, List.of(3L))).thenReturn(List.of(item));
        when(userVoucherRepository.findWithLockByIdAndUserId(21L, 1L)).thenReturn(Optional.of(voucher));
        when(bookRepository.holdStock(10L, 2)).thenReturn(1);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1002L);
            return order;
        });
        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CheckoutRequestDTO request = request();
        request.setUserVoucherId(21L);

        var response = checkoutService.checkout(1L, "voucher-key", request);

        var orderCaptor = forClass(Order.class);
        verify(orderRepository).saveAndFlush(orderCaptor.capture());
        assertThat(response.getDiscountAmount()).isEqualTo(50_000L);
        assertThat(response.getTotal()).isEqualTo(480_000L);
        assertThat(orderCaptor.getValue().getUserVoucher()).isSameAs(voucher);
        assertThat(voucher.getStatus()).isEqualTo(UserVoucherStatus.USED);
        assertThat(voucher.getUsedAt()).isNotNull();
        verify(userVoucherRepository).save(voucher);
    }

    @Test
    void checkoutRejectsEmptyCart() {
        User user = customer();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "key-1")).thenReturn(Optional.empty());
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address(user)));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.checkout(1L, "key-1", request()))
                .isInstanceOf(CartEmptyException.class);
    }

    @Test
    void guestCheckoutDoesNotMisreportAnUnrelatedDatabaseFailureAsIdempotencyConflict() {
        GuestCartItemDTO item = new GuestCartItemDTO();
        item.setBookId(10L);
        item.setQuantity(1);
        GuestCheckoutRequestDTO request = new GuestCheckoutRequestDTO();
        request.setEmail("guest@example.com");
        request.setRecipient("Guest User");
        request.setPhone("0900000000");
        request.setLine("123 Street");
        request.setCity("Ho Chi Minh City");
        request.setItems(List.of(item));
        request.setDeliveryType(DeliveryType.SELF);

        when(orderRepository.findByGuestEmailAndIdempotencyKey("guest@example.com", "guest-key"))
                .thenReturn(Optional.empty());
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book(250000, 5)));
        when(bookRepository.holdStock(10L, 1)).thenReturn(1);
        DataIntegrityViolationException databaseFailure =
                new DataIntegrityViolationException("orders.user_id must not be null");
        when(orderRepository.saveAndFlush(any(Order.class))).thenThrow(databaseFailure);

        assertThatThrownBy(() -> checkoutService.checkoutGuest("guest-key", request))
                .isSameAs(databaseFailure);
    }

    @Test
    void guestCheckoutWithoutEmailReturnsExistingOrderOnRetry() {
        GuestCartItemDTO item = new GuestCartItemDTO();
        item.setBookId(10L);
        item.setQuantity(1);
        GuestCheckoutRequestDTO request = new GuestCheckoutRequestDTO();
        request.setEmail(null);
        request.setRecipient("Guest User");
        request.setPhone("0900000000");
        request.setLine("123 Street");
        request.setCity("Ho Chi Minh City");
        request.setItems(List.of(item));
        request.setDeliveryType(DeliveryType.SELF);

        Order existingOrder = Order.builder()
                .id(77L)
                .status(OrderStatus.PENDING_PAYMENT)
                .subtotal(250_000L)
                .shippingFee(30_000L)
                .deliveryType(DeliveryType.SELF)
                .giftWrapFee(0L)
                .discountAmount(0L)
                .total(280_000L)
                .build();
        Payment payment = Payment.builder()
                .id(88L)
                .status(PaymentStatus.PENDING)
                .providerOrderCode(123456L)
                .checkoutUrl("https://sandbox.vnpayment.vn/pay")
                .build();
        when(orderRepository.findByIdempotencyKeyAndUserIsNullAndGuestEmailIsNull("guest-key"))
                .thenReturn(Optional.of(existingOrder));
        when(paymentRepository.findByOrderId(77L)).thenReturn(Optional.of(payment));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(77L)).thenReturn(List.of());

        var response = checkoutService.checkoutGuest("guest-key", request);

        assertThat(response.getOrderId()).isEqualTo(77L);
        assertThat(response.getCheckoutUrl()).isEqualTo("https://sandbox.vnpayment.vn/pay");
        verify(bookRepository, never()).holdStock(anyLong(), anyInt());
        verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    private CheckoutRequestDTO request() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setAddressId(5L);
        request.setCartItemIds(List.of(3L));
        return request;
    }

    private User customer() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("User")
                .role(UserRole.CUSTOMER)
                .enabled(true)
                .emailVerified(true)
                .build();
    }

    private Address address(User user) {
        return Address.builder()
                .id(5L)
                .user(user)
                .recipient("Nguyen Van A")
                .phone("0900000000")
                .line("123 Street")
                .city("Ho Chi Minh")
                .build();
    }

    private Book book(long price, int stock) {
        return Book.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .category(Category.builder().id(2L).name("Programming").slug("programming").active(true).build())
                .price(price)
                .stock(stock)
                .active(true)
                .build();
    }

    private UserVoucher userVoucher(User user, DiscountType type, long value, long minimum) {
        return UserVoucher.builder()
                .id(21L)
                .user(user)
                .voucher(Voucher.builder()
                        .id(2L)
                        .name("Demo voucher")
                        //.codePrefix("DEMO")
                        .discountType(type)
                        .discountValue(value)
                        .minOrderValue(minimum)
                        .status(VoucherStatus.ACTIVE)
                        .build())
                //.code("DEMO-0001")
                .status(UserVoucherStatus.UNUSED)
                .expiresAt(Instant.now().plusSeconds(3_600))
                .build();
    }
}
