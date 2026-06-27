package com.bookverse.integration.rag.dto;

import java.util.List;

public record RagQueryResponse(
        String answer,
        List<RagSource> sources,
        Usage usage
) {
    public record Usage(
            int promptTokens,
            int completionTokens,
            int totalTokens
    ) {}
}
