package com.bookverse.integration.rag.dto;

import java.util.List;

public record RagCatalogRecommendRequest(
        String query,
        List<RagCatalogRecommendItem> books,
        List<RagChatHistoryMessage> history
) {}
