package com.bookverse.dto.request.ai;

public record ChatHistoryMessage(
        String role,
        String content
) {}
