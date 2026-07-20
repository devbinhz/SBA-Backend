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

    @Override
    public RagDeleteIndexResponse deleteIndex(Long bookId) {
        try {
            return restClient.delete()
                    .uri("/index/{bookId}", bookId)
                    .retrieve()
                    .body(RagDeleteIndexResponse.class);
        } catch (Exception ex) {
            throw new RagUnavailableException("Failed to delete RAG index: " + ex.getMessage());
        }
    }

    @Override
    public RagIndexStatusResponse getIndexStatus(Long bookId) {
        try {
            return restClient.get()
                    .uri("/index/{bookId}/status", bookId)
                    .retrieve()
                    .body(RagIndexStatusResponse.class);
        } catch (Exception ex) {
            throw new RagUnavailableException("Failed to get RAG index status: " + ex.getMessage());
        }
    }

    @Override
    public RagCatalogUpsertResponse catalogUpsert(RagCatalogUpsertRequest request) {
        try {
            return restClient.post()
                    .uri("/catalog/upsert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RagCatalogUpsertResponse.class);
        } catch (Exception ex) {
            throw new RagUnavailableException("Failed to upsert RAG catalog: " + ex.getMessage());
        }
    }

    @Override
    public RagCatalogSearchResponse catalogSearch(RagCatalogSearchRequest request) {
        try {
            return restClient.post()
                    .uri("/catalog/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RagCatalogSearchResponse.class);
        } catch (Exception ex) {
            throw new RagUnavailableException("Failed to search RAG catalog: " + ex.getMessage());
        }
    }

    @Override
    public void deleteCatalog(Long bookId) {
        try {
            restClient.delete()
                    .uri("/catalog/{bookId}", bookId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new RagUnavailableException("Failed to delete RAG catalog: " + ex.getMessage());
        }
    }

    @Override
    public RagCatalogStatusResponse getCatalogStatus(Long bookId) {
        try {
            return restClient.get()
                    .uri("/catalog/{bookId}/status", bookId)
                    .retrieve()
                    .body(RagCatalogStatusResponse.class);
        } catch (Exception ex) {
            throw new RagUnavailableException("Failed to get RAG catalog status: " + ex.getMessage());
        }
    }

    @Override
    public RagCatalogRecommendResponse catalogRecommend(RagCatalogRecommendRequest request) {
        try {
            return restClient.post()
                    .uri("/catalog/recommend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RagCatalogRecommendResponse.class);
        } catch (Exception ex) {
            throw new RagUnavailableException("Failed to get RAG book recommendations: " + ex.getMessage());
        }
    }
}
