package com.bookverse.dto.request.ai;

import jakarta.validation.constraints.NotBlank;

public record AiRecommendRequest(
        @NotBlank String query,
        Integer topK
) {}
