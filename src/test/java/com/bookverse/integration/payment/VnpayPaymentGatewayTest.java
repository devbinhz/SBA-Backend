package com.bookverse.integration.payment;

import com.bookverse.common.exception.BadRequestException;
import com.bookverse.config.VnpayProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VnpayPaymentGatewayTest {

    private final VnpayPaymentGateway gateway = new VnpayPaymentGateway(new VnpayProperties(
            "demo",
            "test-secret",
            "https://sandbox.vnpay.test/pay",
            "http://localhost:5173/payment/result",
            "http://localhost:5173/payment/cancel"
    ));

    @Test
    void webhookAmountIsConvertedFromVnpayMinorUnitsToVnd() {
        PaymentWebhookResult result = gateway.verifyWebhook(new PaymentWebhookCommand(Map.of(
                "vnp_TxnRef", "1001001",
                "vnp_Amount", "18000000"
        )));

        assertThat(result.signatureValid()).isFalse();
        assertThat(result.amount()).isEqualTo(180000L);
    }

    @Test
    void malformedWebhookAmountIsRejected() {
        assertThatThrownBy(() -> gateway.verifyWebhook(new PaymentWebhookCommand(Map.of(
                "vnp_TxnRef", "1001001",
                "vnp_Amount", "18000001"
        ))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid VNPAY amount");
    }
}
