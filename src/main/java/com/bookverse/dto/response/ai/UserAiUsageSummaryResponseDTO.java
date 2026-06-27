package com.bookverse.dto.response.ai;

import java.time.Instant;

public record UserAiUsageSummaryResponseDTO(
        Long userId,
        String fullName,
        String email,
        Long requestCount,
        Long totalPromptTokens,
        Long totalCompletionTokens,
        Long totalTokens,
        Long totalDurationMs,
        Instant lastUsedAt
) {}
