package com.bookverse.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "bookverse.security")
@Validated
public record BookverseSecurityProperties(@NotBlank String jwtSecret) {
}

