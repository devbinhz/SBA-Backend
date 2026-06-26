package com.bookverse.integration.rag;

import com.bookverse.common.exception.RagUnavailableException;
import com.bookverse.integration.rag.dto.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FastApiRagClient implements RagClient {

    private final RestClient restClient;

    public FastApiRagClient(@Qualifier("ragRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public RagHealthResponse checkHealth() {
        try {
            return restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(RagHealthResponse.class);
        } catch (Exception ex) {
            throw new RagUnavailableException("RAG service is down or unreachable: " + ex.getMessage());
        }
    }

    @Override
    public RagIngestResponse ingest(RagIngestRequest request) {
        try {
            return restClient.post()
                    .uri("/ingest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RagIngestResponse.class);
        } catch (Exception ex) {
            throw new RagUnavailableException("Failed to request RAG ingestion: " + ex.getMessage());
        }
    }

    @Override
    public RagQueryResponse query(RagQueryRequest request) {
        try {
            return restClient.post()
                    .uri("/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RagQueryResponse.class);
        } catch (Exception ex) {
            throw new RagUnavailableException("Failed to query RAG service: " + ex.getMessage());
        }
    }
}
