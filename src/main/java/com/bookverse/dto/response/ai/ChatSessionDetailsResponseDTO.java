package com.bookverse.dto.response.ai;

import com.bookverse.enums.AiRequestType;
import java.time.Instant;
import java.util.List;

public record ChatSessionDetailsResponseDTO(
        Long id,
        String title,
        AiRequestType sessionType,
        List<Long> bookIds,
        List<ChatMessageResponseDTO> messages,
        Instant createdAt,
        Instant updatedAt
) {}
