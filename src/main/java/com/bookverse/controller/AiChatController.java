package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.response.ai.AiChatResponse;
import com.bookverse.integration.rag.RagClient;
import com.bookverse.integration.rag.dto.RagHealthResponse;
import com.bookverse.integration.rag.dto.RagIngestRequest;
import com.bookverse.integration.rag.dto.RagIngestResponse;
import com.bookverse.service.ai.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Chat", description = "AI Chat APIs")
public class AiChatController {

    private final AiChatService aiChatService;
    private final RagClient ragClient;

    public AiChatController(AiChatService aiChatService, RagClient ragClient) {
        this.aiChatService = aiChatService;
        this.ragClient = ragClient;
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI about books")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ApiResponse.success(aiChatService.chat(request));
    }

    @GetMapping("/health")
    @Operation(summary = "Check the health of the RAG service (Admin)")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<RagHealthResponse> checkHealth() {
        return ApiResponse.success(ragClient.checkHealth());
    }

    @PostMapping("/ingest")
    @Operation(summary = "Trigger ingestion in the RAG service (Admin)")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<RagIngestResponse> ingest(@Valid @RequestBody RagIngestRequest request) {
        return ApiResponse.success(aiChatService.ingest(request));
    }
}
