package com.bookverse.integration.payment;

import java.util.Map;

public record PaymentWebhookCommand(Map<String, String> params) {
}
