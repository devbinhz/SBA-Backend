package com.bookverse.dto.response.ai;

import java.time.Instant;
import java.util.List;

public record ChatMessageResponseDTO(
        Long id,
        String role,
        String content,
        List<ChatSource> sources,
        Instant createdAt
) {}
