package com.bookverse.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "bookverse.vnpay")
@Validated
public record VnpayProperties(
        @NotBlank String tmnCode,
        @NotBlank String hashSecret,
        @NotBlank String paymentUrl,
        @NotBlank String returnUrl,
        String cancelUrl
) {
}
