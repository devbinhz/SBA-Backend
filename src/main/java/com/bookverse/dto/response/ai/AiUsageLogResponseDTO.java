package com.bookverse.dto.response.ai;

import com.bookverse.enums.AiRequestType;
import java.time.Instant;

public record AiUsageLogResponseDTO(
        Long id,
        Long userId,
        String userEmail,
        String userFullName,
        AiRequestType requestType,
        String query,
        String response,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        Long durationMs,
        Instant createdAt
) {}
