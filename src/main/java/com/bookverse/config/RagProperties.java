package com.bookverse.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "bookverse.rag")
@Validated
public record RagProperties(
        @NotBlank String baseUrl,
        @NotNull Integer timeoutMs
) {
}
