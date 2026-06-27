package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagCatalogSearchRequest(
        String query,
        @JsonProperty("top_k") Integer topK
) {}
