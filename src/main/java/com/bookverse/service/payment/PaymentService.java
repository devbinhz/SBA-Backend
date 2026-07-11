package com.bookverse.service.payment;

import com.bookverse.dto.request.checkout.CheckoutRequestDTO;
import com.bookverse.dto.response.checkout.CheckoutResponseDTO;
import com.bookverse.dto.response.payment.PendingPaymentLinkResponseDTO;
import com.bookverse.dto.response.payment.PaymentWebhookResponseDTO;

import java.util.Map;

public interface PaymentService {

    CheckoutResponseDTO checkout(Long userId, String idempotencyKey, CheckoutRequestDTO request, String clientIp);

    PendingPaymentLinkResponseDTO getPendingPaymentLink(Long userId, Long orderId);

    PaymentWebhookResponseDTO handleVnpayWebhook(Map<String, String> params);
}
