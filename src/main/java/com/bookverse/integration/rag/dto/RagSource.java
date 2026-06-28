package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagSource(
        @JsonProperty("book_id") Long bookId,
        @JsonProperty("document_name") String documentName,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("file_type") String fileType,
        @JsonProperty("chunk_index") Integer chunkIndex,
        Integer page,
        Double score,
        String text
) {}
