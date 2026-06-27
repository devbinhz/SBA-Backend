package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagDeleteIndexResponse(
        @JsonProperty("book_id") Long bookId,
        @JsonProperty("deleted_chunks") Integer deletedChunks
) {}
