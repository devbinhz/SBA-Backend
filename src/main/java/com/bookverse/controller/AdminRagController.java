package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.integration.rag.RagClient;
import com.bookverse.integration.rag.dto.RagHealthResponse;
import com.bookverse.integration.rag.dto.RagIndexStatusResponse;
import com.bookverse.integration.rag.dto.RagIngestResponse;
import com.bookverse.integration.rag.dto.RagCatalogStatusResponse;
import com.bookverse.service.ai.AdminRagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/rag")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Book", description = "Book Management APIs")
public class AdminRagController {

    private final AdminRagService adminRagService;
    private final RagClient ragClient;

    public AdminRagController(AdminRagService adminRagService, RagClient ragClient) {
        this.adminRagService = adminRagService;
        this.ragClient = ragClient;
    }

    @PostMapping("/ingest/{bookId}")
    @Operation(summary = "Ingest a single book's content into RAG")
    public ApiResponse<RagIngestResponse> ingestBook(@PathVariable Long bookId) {
        return ApiResponse.success(adminRagService.ingestBookContent(bookId));
    }

    @PostMapping("/ingest/bulk")
    @Operation(summary = "Ingest multiple books' content into RAG in bulk")
    public ApiResponse<RagIngestResponse> ingestBooksInBulk(@RequestBody List<Long> bookIds) {
        return ApiResponse.success(adminRagService.ingestBooksContent(bookIds));
    }

    @DeleteMapping("/index/{bookId}")
    @Operation(summary = "Delete book index from RAG content and catalog")
    public ApiResponse<Void> deleteBookIndex(@PathVariable Long bookId) {
        adminRagService.deleteBookIndex(bookId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/index/bulk")
    @Operation(summary = "Delete multiple books' indices in bulk")
    public ApiResponse<Void> deleteBooksIndicesInBulk(@RequestBody List<Long> bookIds) {
        adminRagService.deleteBooksIndices(bookIds);
        return ApiResponse.success(null);
    }

    @GetMapping("/index/{bookId}/status")
    @Operation(summary = "Get RAG indexing status of a book")
    public ApiResponse<RagIndexStatusResponse> getBookIndexStatus(@PathVariable Long bookId) {
        return ApiResponse.success(adminRagService.getIndexStatus(bookId));
    }

    @GetMapping("/catalog/{bookId}/status")
    @Operation(summary = "Get RAG catalog sync status of a book")
    public ApiResponse<RagCatalogStatusResponse> getBookCatalogStatus(@PathVariable Long bookId) {
        return ApiResponse.success(adminRagService.getCatalogStatus(bookId));
    }

    @PostMapping("/catalog/upsert/{bookId}")
    @Operation(summary = "Upsert a single book's metadata into the RAG catalog")
    public ApiResponse<Void> upsertBookCatalog(@PathVariable Long bookId) {
        adminRagService.upsertBookCatalog(bookId);
        return ApiResponse.success(null);
    }

    @PostMapping("/catalog/upsert/bulk")
    @Operation(summary = "Upsert multiple books' metadata into the RAG catalog in bulk")
    public ApiResponse<Void> upsertBooksCatalogInBulk(@RequestBody List<Long> bookIds) {
        adminRagService.upsertBooksCatalog(bookIds);
        return ApiResponse.success(null);
    }

    @GetMapping("/health")
    @Operation(summary = "Check the health of the RAG service")
    public ApiResponse<RagHealthResponse> checkHealth() {
        return ApiResponse.success(ragClient.checkHealth());
    }
}
