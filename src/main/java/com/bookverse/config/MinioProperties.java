package com.bookverse.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "bookverse.minio")
@Validated
public record MinioProperties(
        @NotBlank String endpoint,
        String publicEndpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String booksBucket,
        @NotBlank String thumbnailsBucket
) {
    public String publicEndpoint() {
        return publicEndpoint != null && !publicEndpoint.isBlank() ? publicEndpoint : endpoint;
    }
}
