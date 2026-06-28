package com.bookverse.integration.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RagIngestRequest(
        List<RagIngestItem> items,
        @JsonProperty("chunk_size") Integer chunkSize,
        @JsonProperty("overlap_size") Integer overlapSize
) {}
