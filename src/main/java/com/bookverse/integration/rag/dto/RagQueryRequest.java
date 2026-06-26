package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagQueryRequest(
        String query,
        @JsonProperty("book_id") Long bookId,
        @JsonProperty("top_k") Integer topK
) {}
