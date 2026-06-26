package com.bookverse.dto.response.ai;

public record ChatSource(
        Long bookId,
        String bookTitle,
        String fileName,
        Integer page,
        Double score,
        String text
) {}
