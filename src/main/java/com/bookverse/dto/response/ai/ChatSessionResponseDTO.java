package com.bookverse.dto.response.ai;

import com.bookverse.enums.AiRequestType;
import java.time.Instant;
import java.util.List;

public record ChatSessionResponseDTO(
        Long id,
        String title,
        AiRequestType sessionType,
        List<Long> bookIds,
        Instant createdAt,
        Instant updatedAt
) {}
