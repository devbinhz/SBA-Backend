package com.bookverse.integration.rag.dto;

import java.util.List;

public record RagIngestRequest(
        List<RagIngestItem> items
) {}
