package com.bookverse.integration.payment;

public record PaymentLinkResult(
        String providerPaymentLinkId,
        String checkoutUrl
) {
}
