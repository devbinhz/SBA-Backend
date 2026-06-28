package com.bookverse.dto.request.ai;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequestDTO(
        @NotBlank String content
) {}
