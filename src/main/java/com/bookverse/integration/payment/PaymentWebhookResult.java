package com.bookverse.integration.payment;

public record PaymentWebhookResult(
        boolean signatureValid,
        String dedupeKey,
        String eventType,
        Long providerOrderCode,
        String transactionId,
        boolean success,
        String responseCode,
        String transactionStatus
) {
}
