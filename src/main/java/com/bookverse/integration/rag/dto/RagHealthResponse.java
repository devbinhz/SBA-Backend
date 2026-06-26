package com.bookverse.integration.rag.dto;

public record RagHealthResponse(
        String status,
        String qdrant,
        String mongo,
        String minio
) {}
