package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagCatalogRecommendItem(
        Long id,
        String title,
        String author,
        String description,
        Long price,
        String publisher,
        @JsonProperty("publication_year") Integer publicationYear,
        String language,
        Integer pages,
        Integer stock,
        String category
) {}
