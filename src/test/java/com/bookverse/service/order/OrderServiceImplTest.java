package com.bookverse.service.order;

import com.bookverse.common.exception.ConflictException;
import com.bookverse.common.exception.ForbiddenException;
import com.bookverse.dto.request.order.UpdateOrderStatusRequestDTO;
import com.bookverse.entity.Book;
import com.bookverse.entity.Order;
import com.bookverse.entity.OrderItem;
import com.bookverse.entity.Payment;
import com.bookverse.entity.User;
import com.bookverse.entity.UserVoucher;
import com.bookverse.enums.OrderStatus;
import com.bookverse.enums.PaymentProvider;
import com.bookverse.enums.PaymentStatus;
import com.bookverse.enums.UserRole;
import com.bookverse.enums.VoucherStatus;
import com.bookverse.mapper.OrderMapper;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.OrderItemRepository;
import com.bookverse.repository.OrderRepository;
import com.bookverse.repository.OrderStatusHistoryRepository;
import com.bookverse.repository.PaymentRepository;
import com.bookverse.repository.StockMovementRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.order.impl.OrderServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    private PaymentRepository paymentRepository;
    private BookRepository bookRepository;
    private StockMovementRepository stockMovementRepository;
    private UserRepository userRepository;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        orderStatusHistoryRepository = mock(OrderStatusHistoryRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        bookRepository = mock(BookRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        userRepository = mock(UserRepository.class);
        orderService = new OrderServiceImpl(
                orderRepository,
                orderItemRepository,
                orderStatusHistoryRepository,
                paymentRepository,
                bookRepository,
                stockMovementRepository,
                userRepository,
                new OrderMapper(new ObjectMapper())
        );
    }

    @Test
    void getOrderRejectsDifferentCustomer() {
        Order order = order(1001L, customer(2L), OrderStatus.PAID);
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrder(1L, UserRole.CUSTOMER, 1001L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void customerCancelsPendingOrderAndReleasesStockOnce() {
        User user = customer(1L);
        Order order = order(1001L, user, OrderStatus.PENDING_PAYMENT);
        UserVoucher voucher = usedVoucher();
        order.setUserVoucher(voucher);
        Book book = Book.builder().id(10L).title("Clean Code").build();
        Payment payment = Payment.builder().id(501L).order(order).status(PaymentStatus.PENDING).build();
        when(orderRepository.findWithLockById(1001L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(1001L)).thenReturn(Optional.of(payment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(1001L))
                .thenReturn(List.of(OrderItem.builder().id(1L).order(order).book(book).quantity(2).unitPrice(100L).lineTotal(200L).titleSnapshot("Clean Code").build()));
        when(stockMovementRepository.existsByOperationKey("order:1001:release:10")).thenReturn(false);
        when(bookRepository.adjustStockAtomic(10L, 2)).thenReturn(1);

        var response = orderService.cancelPendingOrder(1L, 1001L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();
        assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.UNUSED);
        assertThat(voucher.getUsedAt()).isNull();
        verify(bookRepository).adjustStockAtomic(10L, 2);
        verify(stockMovementRepository).save(any());
        verify(orderStatusHistoryRepository).save(any());
    }

    @Test
    void expirySchedulerCancelsExpiredPendingOrderExpiresPaymentAndReleasesStockOnce() {
        User user = customer(1L);
        Order order = order(1001L, user, OrderStatus.PENDING_PAYMENT);
        UserVoucher voucher = usedVoucher();
        order.setUserVoucher(voucher);
        order.setExpiresAt(Instant.now().minusSeconds(60));
        Book book = Book.builder().id(10L).title("Clean Code").build();
        Payment payment = Payment.builder().id(501L).order(order).status(PaymentStatus.PENDING).build();
        when(orderRepository.findExpiredPendingOrderIds(any(), any())).thenReturn(List.of(1001L));
        when(paymentRepository.findWithLockByOrderId(1001L)).thenReturn(Optional.of(payment));
        when(orderRepository.findWithLockById(1001L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(1001L))
                .thenReturn(List.of(OrderItem.builder().id(1L).order(order).book(book).quantity(2).unitPrice(100L).lineTotal(200L).titleSnapshot("Clean Code").build()));
        when(stockMovementRepository.existsByOperationKey("order:1001:release:10")).thenReturn(false);
        when(bookRepository.adjustStockAtomic(10L, 2)).thenReturn(1);

        int expired = orderService.expirePendingOrders(50);

        assertThat(expired).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(order.getCancelledAt()).isNotNull();
        assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.UNUSED);
        assertThat(voucher.getUsedAt()).isNull();
        verify(bookRepository).adjustStockAtomic(10L, 2);
        verify(stockMovementRepository).save(any());
        verify(orderStatusHistoryRepository).save(any());
    }

    @Test
    void expirySchedulerReleasesStockForGuestOrderWithoutCustomerAccount() {
        Order order = order(1002L, null, OrderStatus.PENDING_PAYMENT);
        order.setGuestEmail("guest@example.com");
        order.setExpiresAt(Instant.now().minusSeconds(60));
        Book book = Book.builder().id(11L).title("Refactoring").build();
        Payment payment = Payment.builder().id(502L).order(order).status(PaymentStatus.PENDING).build();
        when(orderRepository.findExpiredPendingOrderIds(any(), any())).thenReturn(List.of(1002L));
        when(paymentRepository.findWithLockByOrderId(1002L)).thenReturn(Optional.of(payment));
        when(orderRepository.findWithLockById(1002L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(1002L))
                .thenReturn(List.of(OrderItem.builder().id(2L).order(order).book(book).quantity(1)
                        .unitPrice(120L).lineTotal(120L).titleSnapshot("Refactoring").build()));
        when(stockMovementRepository.existsByOperationKey("order:1002:release:11")).thenReturn(false);
        when(bookRepository.adjustStockAtomic(11L, 1)).thenReturn(1);

        int expired = orderService.expirePendingOrders(50);

        assertThat(expired).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        verify(bookRepository).adjustStockAtomic(11L, 1);
        verify(stockMovementRepository).save(any());
    }

    @Test
    void expirySchedulerSkipsWhenWebhookAlreadyChangedState() {
        User user = customer(1L);
        Order order = order(1001L, user, OrderStatus.PAID);
        order.setExpiresAt(Instant.now().minusSeconds(60));
        Payment payment = Payment.builder().id(501L).order(order).status(PaymentStatus.PAID).build();
        when(orderRepository.findExpiredPendingOrderIds(any(), any())).thenReturn(List.of(1001L));
        when(paymentRepository.findWithLockByOrderId(1001L)).thenReturn(Optional.of(payment));
        when(orderRepository.findWithLockById(1001L)).thenReturn(Optional.of(order));

        int expired = orderService.expirePendingOrders(50);

        assertThat(expired).isZero();
        verify(bookRepository, never()).adjustStockAtomic(anyLong(), anyInt());
        verify(stockMovementRepository, never()).save(any());
        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    void adminCannotSkipOrderState() {
        Order order = order(1001L, customer(1L), OrderStatus.PAID);
        when(orderRepository.findWithLockById(1001L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(99L, 1001L, UpdateOrderStatusRequestDTO.builder()
                        .status(OrderStatus.SHIPPED)
                        .shippingProvider("GHN")
                        .trackingCode("TRACK-1")
                        .build()))
                .isInstanceOf(ConflictException.class);

        verify(bookRepository, never()).incrementSoldCountAtomic(anyLong(), anyInt());
    }

    @Test
    void adminShippingRequiresProviderAndTracking() {
        Order order = order(1001L, customer(1L), OrderStatus.PROCESSING);
        when(orderRepository.findWithLockById(1001L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(99L, 1001L, UpdateOrderStatusRequestDTO.builder()
                        .status(OrderStatus.SHIPPED)
                        .shippingProvider("GHN")
                        .build()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deliveredIncrementsSoldCountOnceThroughTerminalTransition() {
        Order order = order(1001L, customer(1L), OrderStatus.SHIPPED);
        Book book = Book.builder().id(10L).title("Clean Code").build();
        when(orderRepository.findWithLockById(1001L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(1001L))
                .thenReturn(List.of(OrderItem.builder().id(1L).order(order).book(book).quantity(2).unitPrice(100L).lineTotal(200L).titleSnapshot("Clean Code").build()));
        when(bookRepository.incrementSoldCountAtomic(10L, 2)).thenReturn(1);

        var response = orderService.updateStatus(99L, 1001L, UpdateOrderStatusRequestDTO.builder()
                .status(OrderStatus.DELIVERED)
                .build());

        assertThat(response.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
        verify(bookRepository).incrementSoldCountAtomic(10L, 2);

        assertThatThrownBy(() -> orderService.updateStatus(99L, 1001L, UpdateOrderStatusRequestDTO.builder()
                        .status(OrderStatus.DELIVERED)
                        .build()))
                .isInstanceOf(ConflictException.class);
    }

    private Order order(Long id, User user, OrderStatus status) {
        return Order.builder()
                .id(id)
                .user(user)
                .status(status)
                .subtotal(200L)
                .shippingFee(30L)
                .total(230L)
                .addressSnapshot("{\"recipient\":\"Nguyen Van A\"}")
                .paymentMethod(PaymentProvider.VNPAY)
                .idempotencyKey("key-" + id)
                .build();
    }

    private User customer(Long id) {
        return User.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .fullName("User " + id)
                .passwordHash("hash")
                .role(UserRole.CUSTOMER)
                .enabled(true)
                .emailVerified(true)
                .build();
    }

    private UserVoucher usedVoucher() {
        return UserVoucher.builder()
                .id(21L)
                .code("DEMO-USED")
                .status(VoucherStatus.USED)
                .expiresAt(Instant.now().plusSeconds(3_600))
                .usedAt(Instant.now())
                .build();
    }
}
