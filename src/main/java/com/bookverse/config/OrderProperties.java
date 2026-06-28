package com.bookverse.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "bookverse.order")
@Validated
public record OrderProperties(
        @Min(0) long shippingFeeVnd,
        @Min(1) long expirationMinutes
) {
}
