package com.bookverse.dto.request.ai;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AiRecommendRequest(
        @NotBlank String query,
        Integer topK,
        List<ChatHistoryMessage> history
) {}
