package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagCatalogStatusResponse(
        @JsonProperty("book_id") Long bookId,
        String status
) {}
