package com.bookverse.service.checkout;

import com.bookverse.common.exception.CartEmptyException;
import com.bookverse.config.OrderProperties;
import com.bookverse.dto.request.checkout.CheckoutRequestDTO;
import com.bookverse.entity.Address;
import com.bookverse.entity.Book;
import com.bookverse.entity.Cart;
import com.bookverse.entity.CartItem;
import com.bookverse.entity.Category;
import com.bookverse.entity.Order;
import com.bookverse.entity.Payment;
import com.bookverse.entity.User;
import com.bookverse.enums.PaymentProvider;
import com.bookverse.enums.PaymentStatus;
import com.bookverse.enums.UserRole;
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
import com.bookverse.service.checkout.impl.CheckoutServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
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
    private CheckoutServiceImpl checkoutService;

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
                new OrderProperties(30000, 15),
                new ObjectMapper()
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
        when(cartItemRepository.findByCartIdOrderByIdAsc(9L)).thenReturn(List.of(item));

        var response = checkoutService.preview(1L, request());

        assertThat(response.getSubtotal()).isEqualTo(500000);
        assertThat(response.getShippingFee()).isEqualTo(30000);
        assertThat(response.getTotal()).isEqualTo(530000);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void checkoutHoldsStockCreatesPendingPaymentAndClearsCart() {
        User user = customer();
        Address address = address(user);
        Cart cart = Cart.builder().id(9L).user(user).build();
        Book book = book(250000, 5);
        CartItem item = CartItem.builder().id(3L).cart(cart).book(book).quantity(2).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdAndIdempotencyKey(1L, "key-1")).thenReturn(Optional.empty());
        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByIdAsc(9L)).thenReturn(List.of(item));
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

        var response = checkoutService.checkout(1L, " key-1 ", request());

        var orderCaptor = forClass(Order.class);
        var paymentCaptor = forClass(Payment.class);
        assertThat(response.getOrderId()).isEqualTo(1001L);
        assertThat(response.getPaymentId()).isEqualTo(501L);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getProviderOrderCode()).isEqualTo(1001001L);
        assertThat(response.getCheckoutUrl()).isNull();
        verify(orderRepository).saveAndFlush(orderCaptor.capture());
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(orderCaptor.getValue().getAddressSnapshot()).contains("Nguyen Van A", "0900000000", "123 Street", "Ho Chi Minh");
        assertThat(orderCaptor.getValue().getPaymentMethod()).isEqualTo(PaymentProvider.VNPAY);
        assertThat(paymentCaptor.getValue().getProvider()).isEqualTo(PaymentProvider.VNPAY);
        assertThat(paymentCaptor.getValue().getProviderOrderCode()).isEqualTo(1001001L);
        verify(bookRepository).holdStock(10L, 2);
        verify(stockMovementRepository).saveAll(any());
        verify(orderStatusHistoryRepository).save(any());
        verify(cartItemRepository).deleteByCartId(9L);
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

    private CheckoutRequestDTO request() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setAddressId(5L);
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
}
