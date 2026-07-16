package com.bookverse.service.payment;

import com.bookverse.common.exception.ConflictException;
import com.bookverse.common.exception.ForbiddenException;
import com.bookverse.common.exception.PaymentVerificationFailedException;
import com.bookverse.dto.request.checkout.CheckoutRequestDTO;
import com.bookverse.dto.response.checkout.CheckoutResponseDTO;
import com.bookverse.entity.Book;
import com.bookverse.entity.Order;
import com.bookverse.entity.OrderItem;
import com.bookverse.entity.Payment;
import com.bookverse.entity.PaymentEvent;
import com.bookverse.entity.User;
import com.bookverse.entity.UserVoucher;
import com.bookverse.enums.OrderStatus;
import com.bookverse.enums.PaymentStatus;
import com.bookverse.enums.VoucherStatus;
import com.bookverse.integration.payment.PaymentGateway;
import com.bookverse.integration.payment.PaymentLinkResult;
import com.bookverse.integration.payment.PaymentWebhookResult;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.OrderItemRepository;
import com.bookverse.repository.OrderStatusHistoryRepository;
import com.bookverse.repository.PaymentEventRepository;
import com.bookverse.repository.PaymentRepository;
import com.bookverse.repository.StockMovementRepository;
import com.bookverse.service.checkout.CheckoutService;
import com.bookverse.service.payment.impl.PaymentServiceImpl;
import com.bookverse.service.voucher.VoucherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceImplTest {

    private CheckoutService checkoutService;
    private PaymentGateway paymentGateway;
    private PaymentRepository paymentRepository;
    private PaymentEventRepository paymentEventRepository;
    private OrderItemRepository orderItemRepository;
    private BookRepository bookRepository;
    private StockMovementRepository stockMovementRepository;
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    private VoucherService voucherService;
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        checkoutService = mock(CheckoutService.class);
        paymentGateway = mock(PaymentGateway.class);
        paymentRepository = mock(PaymentRepository.class);
        paymentEventRepository = mock(PaymentEventRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        bookRepository = mock(BookRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        orderStatusHistoryRepository = mock(OrderStatusHistoryRepository.class);
        voucherService = mock(VoucherService.class);
        paymentService = new PaymentServiceImpl(
                checkoutService,
                paymentGateway,
                paymentRepository,
                paymentEventRepository,
                orderItemRepository,
                bookRepository,
                stockMovementRepository,
                orderStatusHistoryRepository,
                voucherService,
                new ObjectMapper(),
                new TransactionTemplate(new NoopTransactionManager())
        );
    }

    @Test
    void checkoutCreatesVnpayLinkAfterLocalCheckoutAndSavesUrl() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        CheckoutResponseDTO checkoutResponse = checkoutResponse();
        Payment payment = Payment.builder().id(501L).status(PaymentStatus.PENDING).build();
        when(checkoutService.checkout(1L, "key-1", request)).thenReturn(checkoutResponse);
        when(paymentGateway.createCheckoutLink(any())).thenReturn(new PaymentLinkResult("1001001", "https://vnpay.test/pay"));
        when(paymentRepository.findWithLockById(501L)).thenReturn(Optional.of(payment));

        CheckoutResponseDTO response = paymentService.checkout(1L, "key-1", request, "127.0.0.1");

        assertThat(response.getCheckoutUrl()).isEqualTo("https://vnpay.test/pay");
        assertThat(payment.getCheckoutUrl()).isEqualTo("https://vnpay.test/pay");
        assertThat(payment.getProviderPaymentLinkId()).isEqualTo("1001001");
        verify(paymentGateway).createCheckoutLink(any());
        verify(paymentRepository).save(payment);
    }

    @Test
    void checkoutCompensatesWhenCreateLinkFails() {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        Order order = Order.builder().id(1001L).status(OrderStatus.PENDING_PAYMENT).build();
        Book book = Book.builder().id(10L).title("Clean Code").build();
        Payment payment = Payment.builder().id(501L).order(order).status(PaymentStatus.PENDING).build();
        when(checkoutService.checkout(1L, "key-1", request)).thenReturn(checkoutResponse());
        when(paymentGateway.createCheckoutLink(any())).thenThrow(new IllegalStateException("provider down"));
        when(paymentRepository.findWithLockById(501L)).thenReturn(Optional.of(payment));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(1001L))
                .thenReturn(List.of(OrderItem.builder().order(order).book(book).quantity(2).build()));
        when(stockMovementRepository.existsByOperationKey("order:1001:release:10")).thenReturn(false);
        when(bookRepository.adjustStockAtomic(10L, 2)).thenReturn(1);

        assertThatThrownBy(() -> paymentService.checkout(1L, "key-1", request, "127.0.0.1"))
                .isInstanceOf(ConflictException.class);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(bookRepository).adjustStockAtomic(10L, 2);
        verify(stockMovementRepository).save(any());
        verify(orderStatusHistoryRepository).save(any());
    }

    @Test
    void pendingPaymentLinkIsReturnedToOrderOwner() {
        Order order = Order.builder()
                .id(1001L)
                .user(User.builder().id(1L).build())
                .status(OrderStatus.PENDING_PAYMENT)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        Payment payment = Payment.builder()
                .order(order)
                .status(PaymentStatus.PENDING)
                .checkoutUrl("https://vnpay.test/pay")
                .build();
        when(paymentRepository.findByOrderId(1001L)).thenReturn(Optional.of(payment));

        var response = paymentService.getPendingPaymentLink(1L, 1001L);

        assertThat(response.getOrderId()).isEqualTo(1001L);
        assertThat(response.getCheckoutUrl()).isEqualTo("https://vnpay.test/pay");
    }

    @Test
    void pendingPaymentLinkRejectsAnotherCustomer() {
        Order order = Order.builder()
                .id(1001L)
                .user(User.builder().id(1L).build())
                .status(OrderStatus.PENDING_PAYMENT)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        Payment payment = Payment.builder()
                .order(order)
                .status(PaymentStatus.PENDING)
                .checkoutUrl("https://vnpay.test/pay")
                .build();
        when(paymentRepository.findByOrderId(1001L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPendingPaymentLink(2L, 1001L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void pendingPaymentLinkRejectsExpiredOrder() {
        Order order = Order.builder()
                .id(1001L)
                .user(User.builder().id(1L).build())
                .status(OrderStatus.PENDING_PAYMENT)
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        Payment payment = Payment.builder()
                .order(order)
                .status(PaymentStatus.PENDING)
                .checkoutUrl("https://vnpay.test/pay")
                .build();
        when(paymentRepository.findByOrderId(1001L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPendingPaymentLink(1L, 1001L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void webhookSuccessMarksPendingOrderPaidAndDedupesRepeatEvent() {
        Map<String, String> params = Map.of("vnp_TxnRef", "1001001", "vnp_ResponseCode", "00");
        Order order = Order.builder()
                .id(1001L)
                .status(OrderStatus.PENDING_PAYMENT)
                .user(com.bookverse.entity.User.builder().id(1L).build())
                .total(1000L)
                .build();
        Payment payment = Payment.builder()
                .id(501L)
                .order(order)
                .status(PaymentStatus.PENDING)
                .amount(1000L)
                .providerOrderCode(1001001L)
                .build();
        PaymentWebhookResult result = new PaymentWebhookResult(
                true,
                "vnpay:1001001:txn-1",
                "vnpay.payment",
                1001001L,
                1000L,
                "txn-1",
                true,
                "00",
                "00"
        );
        when(paymentGateway.verifyWebhook(any())).thenReturn(result);
        when(paymentRepository.findByProviderOrderCode(1001001L)).thenReturn(Optional.of(payment));
        when(paymentRepository.findWithLockById(501L)).thenReturn(Optional.of(payment));
        when(paymentEventRepository.findByDedupeKey("vnpay:1001001:txn-1")).thenReturn(Optional.empty());
        when(paymentEventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.handleVnpayWebhook(params);

        assertThat(response.isProcessed()).isTrue();
        assertThat(response.isDuplicate()).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(payment.getTransactionId()).isEqualTo("txn-1");
        verify(orderStatusHistoryRepository).save(any());

        when(paymentEventRepository.findByDedupeKey("vnpay:1001001:txn-1"))
                .thenReturn(Optional.of(PaymentEvent.builder().processed(true).build()));
        var duplicate = paymentService.handleVnpayWebhook(params);

        assertThat(duplicate.isDuplicate()).isTrue();
        verify(paymentRepository).findWithLockById(501L);
        verify(voucherService).awardVoucherToUser(1L, 1000L);
    }

    @Test
    void webhookSuccessWithWrongAmountIsRejectedWithoutMarkingOrderPaid() {
        Map<String, String> params = Map.of("vnp_TxnRef", "1001001", "vnp_Amount", "90000");
        Order order = Order.builder()
                .id(1001L)
                .status(OrderStatus.PENDING_PAYMENT)
                .user(User.builder().id(1L).build())
                .total(1000L)
                .build();
        Payment payment = Payment.builder()
                .id(501L)
                .order(order)
                .status(PaymentStatus.PENDING)
                .amount(1000L)
                .providerOrderCode(1001001L)
                .build();
        PaymentWebhookResult result = new PaymentWebhookResult(
                true,
                "vnpay:1001001:wrong-amount",
                "vnpay.payment",
                1001001L,
                900L,
                "wrong-amount",
                true,
                "00",
                "00"
        );
        when(paymentGateway.verifyWebhook(any())).thenReturn(result);
        when(paymentRepository.findByProviderOrderCode(1001001L)).thenReturn(Optional.of(payment));
        when(paymentRepository.findWithLockById(501L)).thenReturn(Optional.of(payment));
        when(paymentEventRepository.findByDedupeKey("vnpay:1001001:wrong-amount")).thenReturn(Optional.empty());
        when(paymentEventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> paymentService.handleVnpayWebhook(params))
                .isInstanceOf(PaymentVerificationFailedException.class)
                .hasMessageContaining("amount mismatch");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(orderStatusHistoryRepository, never()).save(any());
        verify(voucherService, never()).awardVoucherToUser(any(), any());
    }

    @Test
    void webhookInvalidChecksumIsSavedAndRejected() {
        Map<String, String> params = Map.of("vnp_TxnRef", "1001001");
        Payment payment = Payment.builder().id(501L).providerOrderCode(1001001L).build();
        PaymentWebhookResult result = new PaymentWebhookResult(
                false,
                "vnpay:invalid",
                "vnpay.payment",
                1001001L,
                null,
                null,
                false,
                null,
                null
        );
        when(paymentGateway.verifyWebhook(any())).thenReturn(result);
        when(paymentRepository.findByProviderOrderCode(1001001L)).thenReturn(Optional.of(payment));
        when(paymentEventRepository.findByDedupeKey("vnpay:invalid")).thenReturn(Optional.empty());
        when(paymentEventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> paymentService.handleVnpayWebhook(params))
                .isInstanceOf(PaymentVerificationFailedException.class);

        verify(paymentEventRepository).saveAndFlush(any());
        verify(paymentRepository, never()).findWithLockById(501L);
    }

    @Test
    void webhookFailureCancelsPendingOrderAndReleasesStock() {
        Map<String, String> params = Map.of("vnp_TxnRef", "1001001", "vnp_ResponseCode", "24");
        Order order = Order.builder().id(1001L).status(OrderStatus.PENDING_PAYMENT).user(com.bookverse.entity.User.builder().id(1L).build()).build();
        UserVoucher voucher = UserVoucher.builder()
                .id(21L)
                .code("DEMO-USED")
                .status(VoucherStatus.USED)
                .expiresAt(Instant.now().plusSeconds(3_600))
                .usedAt(Instant.now())
                .build();
        order.setUserVoucher(voucher);
        Book book = Book.builder().id(10L).title("Clean Code").build();
        Payment payment = Payment.builder()
                .id(501L)
                .order(order)
                .status(PaymentStatus.PENDING)
                .providerOrderCode(1001001L)
                .build();
        PaymentWebhookResult result = new PaymentWebhookResult(
                true,
                "vnpay:1001001:cancel",
                "vnpay.payment",
                1001001L,
                null,
                "cancel",
                false,
                "24",
                "02"
        );
        when(paymentGateway.verifyWebhook(any())).thenReturn(result);
        when(paymentRepository.findByProviderOrderCode(1001001L)).thenReturn(Optional.of(payment));
        when(paymentRepository.findWithLockById(501L)).thenReturn(Optional.of(payment));
        when(paymentEventRepository.findByDedupeKey("vnpay:1001001:cancel")).thenReturn(Optional.empty());
        when(paymentEventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderIdOrderByIdAsc(1001L))
                .thenReturn(List.of(OrderItem.builder().order(order).book(book).quantity(2).build()));
        when(stockMovementRepository.existsByOperationKey("order:1001:release:10")).thenReturn(false);
        when(bookRepository.adjustStockAtomic(10L, 2)).thenReturn(1);

        var response = paymentService.handleVnpayWebhook(params);

        assertThat(response.isProcessed()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.UNUSED);
        assertThat(voucher.getUsedAt()).isNull();
        verify(bookRepository).adjustStockAtomic(10L, 2);
        verify(stockMovementRepository).save(any());
    }

    private CheckoutResponseDTO checkoutResponse() {
        return CheckoutResponseDTO.builder()
                .orderId(1001L)
                .paymentId(501L)
                .paymentStatus(PaymentStatus.PENDING)
                .providerOrderCode(1001001L)
                .total(530000L)
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
    }

    private static class NoopTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
