package com.bookverse.service.payment.impl;

import com.bookverse.common.exception.ConflictException;
import com.bookverse.common.exception.ForbiddenException;
import com.bookverse.common.exception.PaymentVerificationFailedException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.checkout.CheckoutRequestDTO;
import com.bookverse.dto.request.checkout.GuestCheckoutRequestDTO;
import com.bookverse.dto.response.checkout.CheckoutResponseDTO;
import com.bookverse.dto.response.payment.PendingPaymentLinkResponseDTO;
import com.bookverse.dto.response.payment.PaymentWebhookResponseDTO;
import com.bookverse.entity.Order;
import com.bookverse.entity.OrderItem;
import com.bookverse.entity.OrderStatusHistory;
import com.bookverse.entity.Payment;
import com.bookverse.entity.PaymentEvent;
import com.bookverse.entity.StockMovement;
import com.bookverse.enums.OrderStatus;
import com.bookverse.enums.PaymentProvider;
import com.bookverse.enums.PaymentStatus;
import com.bookverse.enums.StockMovementReason;
import com.bookverse.integration.payment.PaymentGateway;
import com.bookverse.integration.payment.PaymentLinkCommand;
import com.bookverse.integration.payment.PaymentWebhookCommand;
import com.bookverse.integration.payment.PaymentWebhookResult;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.OrderItemRepository;
import com.bookverse.repository.OrderStatusHistoryRepository;
import com.bookverse.repository.PaymentEventRepository;
import com.bookverse.repository.PaymentRepository;
import com.bookverse.repository.StockMovementRepository;
import com.bookverse.service.checkout.CheckoutService;
import com.bookverse.service.payment.PaymentService;
import com.bookverse.service.voucher.VoucherService;
import com.bookverse.enums.VoucherStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final CheckoutService checkoutService;
    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookRepository bookRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final VoucherService voucherService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    public CheckoutResponseDTO checkout(Long userId, String idempotencyKey, CheckoutRequestDTO request, String clientIp) {
        CheckoutResponseDTO response = checkoutService.checkout(userId, idempotencyKey, request);
        if (response.getCheckoutUrl() != null || response.getPaymentStatus() != PaymentStatus.PENDING) {
            return response;
        }

        try {
            var link = paymentGateway.createCheckoutLink(new PaymentLinkCommand(
                    response.getPaymentId(),
                    response.getProviderOrderCode(),
                    response.getTotal(),
                    "BookVerse order " + response.getOrderId(),
                    response.getExpiresAt(),
                    clientIp
            ));
            transactionTemplate.executeWithoutResult(status -> {
                Payment payment = paymentRepository.findWithLockById(response.getPaymentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setProviderPaymentLinkId(link.providerPaymentLinkId());
                    payment.setCheckoutUrl(link.checkoutUrl());
                    paymentRepository.save(payment);
                }
            });
            response.setCheckoutUrl(link.checkoutUrl());
            return response;
        } catch (RuntimeException exception) {
            compensateCreateLinkFailure(response.getPaymentId(), exception);
            throw new ConflictException("Unable to create VNPAY checkout link");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PendingPaymentLinkResponseDTO getPendingPaymentLink(Long userId, Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        Order order = payment.getOrder();

        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Order does not belong to current user");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT || payment.getStatus() != PaymentStatus.PENDING) {
            throw new ConflictException("Order is not awaiting payment");
        }
        if (order.getExpiresAt() == null || !order.getExpiresAt().isAfter(Instant.now())) {
            throw new ConflictException("Payment window has expired");
        }
        if (payment.getCheckoutUrl() == null || payment.getCheckoutUrl().isBlank()) {
            throw new ConflictException("Payment link is unavailable");
        }

        return PendingPaymentLinkResponseDTO.builder()
                .orderId(order.getId())
                .checkoutUrl(payment.getCheckoutUrl())
                .expiresAt(order.getExpiresAt())
                .build();
    }

    @Override
    public CheckoutResponseDTO checkoutGuest(String idempotencyKey, GuestCheckoutRequestDTO request, String clientIp) {
        CheckoutResponseDTO response = checkoutService.checkoutGuest(idempotencyKey, request);
        if (response.getCheckoutUrl() != null || response.getPaymentStatus() != PaymentStatus.PENDING) {
            return response;
        }

        try {
            var link = paymentGateway.createCheckoutLink(new PaymentLinkCommand(
                    response.getPaymentId(),
                    response.getProviderOrderCode(),
                    response.getTotal(),
                    "BookVerse guest order " + response.getOrderId(),
                    response.getExpiresAt(),
                    clientIp
            ));
            transactionTemplate.executeWithoutResult(status -> {
                Payment payment = paymentRepository.findWithLockById(response.getPaymentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setProviderPaymentLinkId(link.providerPaymentLinkId());
                    payment.setCheckoutUrl(link.checkoutUrl());
                    paymentRepository.save(payment);
                }
            });
            response.setCheckoutUrl(link.checkoutUrl());
            return response;
        } catch (RuntimeException exception) {
            compensateCreateLinkFailure(response.getPaymentId(), exception);
            throw new ConflictException("Unable to create VNPAY checkout link");
        }
    }

    @Override
    public PaymentWebhookResponseDTO handleVnpayWebhook(Map<String, String> params) {
        PaymentWebhookResult result = paymentGateway.verifyWebhook(new PaymentWebhookCommand(params));
        return transactionTemplate.execute(status -> processWebhook(params, result));
    }

    private PaymentWebhookResponseDTO processWebhook(Map<String, String> params, PaymentWebhookResult result) {
        Payment payment = findPaymentForWebhook(result);
        PaymentEvent duplicate = paymentEventRepository.findByDedupeKey(result.dedupeKey()).orElse(null);
        if (duplicate != null) {
            return PaymentWebhookResponseDTO.builder()
                    .processed(duplicate.isProcessed())
                    .duplicate(true)
                    .status(payment != null ? payment.getStatus().name() : "DUPLICATE")
                    .build();
        }

        PaymentEvent event = saveEvent(payment, params, result);
        if (!result.signatureValid()) {
            event.setProcessingError("Invalid VNPAY checksum");
            paymentEventRepository.save(event);
            throw new PaymentVerificationFailedException("Invalid VNPAY checksum");
        }
        if (payment == null) {
            event.setProcessingError("Payment not found for provider order code");
            paymentEventRepository.save(event);
            throw new ResourceNotFoundException("Payment not found");
        }

        Payment lockedPayment = paymentRepository.findWithLockById(payment.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        Order order = lockedPayment.getOrder();

        if (result.success()) {
            if (!Objects.equals(lockedPayment.getAmount(), result.amount())) {
                event.setProcessingError("VNPAY amount does not match the expected payment amount");
                paymentEventRepository.save(event);
                throw new PaymentVerificationFailedException("VNPAY payment amount mismatch");
            }
            processSuccessfulPayment(lockedPayment, order, result, event);
        } else {
            processFailedOrCancelledPayment(lockedPayment, order, event, result);
        }

        event.setProcessed(true);
        event.setProcessedAt(Instant.now());
        paymentEventRepository.save(event);
        return PaymentWebhookResponseDTO.builder()
                .processed(true)
                .duplicate(false)
                .status(lockedPayment.getStatus().name())
                .build();
    }

    private Payment findPaymentForWebhook(PaymentWebhookResult result) {
        if (result.providerOrderCode() == null) {
            return null;
        }
        return paymentRepository.findByProviderOrderCode(result.providerOrderCode()).orElse(null);
    }

    private PaymentEvent saveEvent(Payment payment, Map<String, String> params, PaymentWebhookResult result) {
        try {
            return paymentEventRepository.saveAndFlush(PaymentEvent.builder()
                    .payment(payment)
                    .provider(PaymentProvider.VNPAY)
                    .eventType(result.eventType())
                    .dedupeKey(result.dedupeKey())
                    .payloadJson(objectMapper.writeValueAsString(params))
                    .signatureValid(result.signatureValid())
                    .processed(false)
                    .build());
        } catch (DataIntegrityViolationException exception) {
            PaymentEvent duplicate = paymentEventRepository.findByDedupeKey(result.dedupeKey())
                    .orElseThrow(() -> exception);
            duplicate.setProcessed(true);
            return duplicate;
        } catch (JsonProcessingException exception) {
            throw new PaymentVerificationFailedException("Invalid VNPAY payload");
        }
    }

    private void processSuccessfulPayment(Payment payment, Order order, PaymentWebhookResult result, PaymentEvent event) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            event.setProcessingError("Late successful payment for order status " + order.getStatus());
            log.warn("Late VNPAY payment ignored: paymentId={}, orderId={}, orderStatus={}, transactionId={}",
                    payment.getId(), order.getId(), order.getStatus(), result.transactionId());
            return;
        }
        Instant now = Instant.now();
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionId(result.transactionId());
        payment.setPaidAt(now);
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(now);
        paymentRepository.save(payment);
        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(OrderStatus.PENDING_PAYMENT)
                .toStatus(OrderStatus.PAID)
                .note("VNPAY webhook confirmed payment")
                .build());
        
        // Award voucher
        if (order.getUser() != null) {
            voucherService.awardVoucherToUser(order.getUser().getId(), order.getTotal());
        }
    }

    private void processFailedOrCancelledPayment(Payment payment, Order order, PaymentEvent event, PaymentWebhookResult result) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            event.setProcessingError("Late failed/cancelled payment for order status " + order.getStatus());
            log.warn("Late VNPAY failure ignored: paymentId={}, orderId={}, orderStatus={}, responseCode={}, transactionStatus={}",
                    payment.getId(), order.getId(), order.getStatus(), result.responseCode(), result.transactionStatus());
            return;
        }
        payment.setStatus(PaymentStatus.CANCELLED);
        cancelPendingOrder(order, "VNPAY webhook cancelled or failed payment", StockMovementReason.ORDER_CANCEL_RELEASE);
        paymentRepository.save(payment);
    }

    private void compensateCreateLinkFailure(Long paymentId, RuntimeException cause) {
        log.warn("VNPAY checkout link creation failed, compensating paymentId={}", paymentId, cause);
        transactionTemplate.executeWithoutResult(status -> {
            Payment payment = paymentRepository.findWithLockById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
                cancelPendingOrder(payment.getOrder(), "VNPAY checkout link creation failed", StockMovementReason.ORDER_CANCEL_RELEASE);
                paymentRepository.save(payment);
            }
        });
    }

    private void cancelPendingOrder(Order order, String note, StockMovementReason releaseReason) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        releaseStock(order, releaseReason);
        
        if (order.getUserVoucher() != null) {
            order.getUserVoucher().setStatus(VoucherStatus.UNUSED);
            order.getUserVoucher().setUsedAt(null);
        }

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(OrderStatus.PENDING_PAYMENT)
                .toStatus(OrderStatus.CANCELLED)
                .note(note)
                .build());
    }

    private void releaseStock(Order order, StockMovementReason reason) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        for (OrderItem item : items) {
            String operationKey = "order:" + order.getId() + ":release:" + item.getBook().getId();
            if (stockMovementRepository.existsByOperationKey(operationKey)) {
                continue;
            }
            int updated = bookRepository.adjustStockAtomic(item.getBook().getId(), item.getQuantity());
            if (updated != 1) {
                throw new ConflictException("Unable to release stock for order " + order.getId());
            }
            stockMovementRepository.save(StockMovement.builder()
                    .book(item.getBook())
                    .orderId(order.getId())
                    .delta(item.getQuantity())
                    .reason(reason)
                    .operationKey(operationKey)
                    .note("Stock released after payment cancellation")
                    .build());
        }
    }
}
