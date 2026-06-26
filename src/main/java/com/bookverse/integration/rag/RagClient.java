package com.bookverse.integration.rag;

import com.bookverse.integration.rag.dto.*;

public interface RagClient {
    RagHealthResponse checkHealth();
    RagIngestResponse ingest(RagIngestRequest request);
    RagQueryResponse query(RagQueryRequest request);
}
