package com.bookverse.integration.rag.dto;

public record RagChatHistoryMessage(
        String role,
        String content
) {}
