package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RagIngestResponse(
        List<IndexedDocument> indexed,
        List<IngestError> errors,
        @JsonProperty("total_chunks") Integer totalChunks
) {
    public record IndexedDocument(
            @JsonProperty("book_id") Long bookId,
            @JsonProperty("file_name") String fileName,
            Integer chunks
    ) {}

    public record IngestError(
            @JsonProperty("book_id") Long bookId,
            String error
    ) {}
}
