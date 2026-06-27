package com.bookverse.integration.rag.dto;

import java.util.List;

public record RagCatalogUpsertRequest(
        List<RagCatalogUpsertItem> items
) {}
