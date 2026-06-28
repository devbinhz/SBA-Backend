package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagCatalogUpsertItem(
        @JsonProperty("book_id") Long bookId,
        String title,
        String author,
        String category,
        String publisher,
        @JsonProperty("publication_year") Integer publicationYear,
        String language,
        Integer pages,
        String description
) {}
