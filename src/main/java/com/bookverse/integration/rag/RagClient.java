package com.bookverse.integration.rag;

import com.bookverse.integration.rag.dto.*;

public interface RagClient {
    RagHealthResponse checkHealth();
    RagIngestResponse ingest(RagIngestRequest request);
    RagQueryResponse query(RagQueryRequest request);
    RagDeleteIndexResponse deleteIndex(Long bookId);
    RagIndexStatusResponse getIndexStatus(Long bookId);
    RagCatalogUpsertResponse catalogUpsert(RagCatalogUpsertRequest request);
    RagCatalogSearchResponse catalogSearch(RagCatalogSearchRequest request);
    void deleteCatalog(Long bookId);
}
