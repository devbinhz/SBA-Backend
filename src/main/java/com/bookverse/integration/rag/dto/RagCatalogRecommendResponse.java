package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RagCatalogRecommendResponse(
        String answer,
        @JsonProperty("recommended_ids") List<Long> recommendedIds
) {}
