package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagIndexStatusResponse(
        @JsonProperty("book_id") Long bookId,
        String status,
        @JsonProperty("chunk_count") Integer chunkCount,
        @JsonProperty("updated_at") String updatedAt,
        String error
) {}
