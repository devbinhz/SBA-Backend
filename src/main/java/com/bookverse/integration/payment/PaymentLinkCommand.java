package com.bookverse.integration.payment;

import java.time.Instant;

public record PaymentLinkCommand(
        Long paymentId,
        Long providerOrderCode,
        Long amount,
        String orderInfo,
        Instant expiresAt,
        String clientIp
) {
}
