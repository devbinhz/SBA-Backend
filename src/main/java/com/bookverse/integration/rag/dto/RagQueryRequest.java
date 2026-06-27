package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RagQueryRequest(
        String query,
        @JsonProperty("book_ids") List<Long> bookIds,
        @JsonProperty("top_k") Integer topK
) {}
