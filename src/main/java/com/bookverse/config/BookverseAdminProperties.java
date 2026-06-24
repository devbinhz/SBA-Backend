package com.bookverse.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "bookverse.admin")
@Validated
public record BookverseAdminProperties(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String fullName
) {
}

