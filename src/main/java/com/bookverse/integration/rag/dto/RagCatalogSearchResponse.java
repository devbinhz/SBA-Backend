package com.bookverse.integration.rag.dto;

import java.util.List;

public record RagCatalogSearchResponse(
        List<RagCatalogBookHit> hits
) {}
