package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagCatalogBookHit(
        @JsonProperty("book_id") Long bookId,
        Double score
) {}
