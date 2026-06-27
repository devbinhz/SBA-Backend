package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagIngestItem(
        @JsonProperty("book_id") Long bookId,
        @JsonProperty("file_path") String filePath,
        @JsonProperty("title") String title
) {}
