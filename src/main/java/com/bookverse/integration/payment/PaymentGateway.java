package com.bookverse.integration.payment;

public interface PaymentGateway {

    PaymentLinkResult createCheckoutLink(PaymentLinkCommand command);

    PaymentWebhookResult verifyWebhook(PaymentWebhookCommand command);
}
