package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RagCatalogSearchRequest(
        String query,
        @JsonProperty("top_k") Integer topK,
        List<RagChatHistoryMessage> history
) {
    public RagCatalogSearchRequest(String query, Integer topK) {
        this(query, topK, null);
    }
}
