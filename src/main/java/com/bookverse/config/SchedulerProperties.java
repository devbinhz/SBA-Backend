package com.bookverse.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "bookverse.scheduler.expired-orders")
@Validated
public record SchedulerProperties(
        @Min(1) long fixedDelayMs,
        @Min(1) int batchSize
) {
}
