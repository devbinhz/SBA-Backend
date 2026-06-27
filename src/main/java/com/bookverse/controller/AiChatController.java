package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.request.ai.AiRecommendRequest;
import com.bookverse.dto.response.ai.AiChatResponse;
import com.bookverse.dto.response.ai.AiRecommendResponse;
import com.bookverse.service.ai.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Chat", description = "AI Chat APIs")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI about books")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal(expression = "user.id") Long userId
    ) {
        return ApiResponse.success(aiChatService.chat(request, userId));
    }

    @PostMapping("/recommend")
    @Operation(summary = "Get book recommendations based on user needs")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<AiRecommendResponse> recommend(
            @Valid @RequestBody AiRecommendRequest request,
            @AuthenticationPrincipal(expression = "user.id") Long userId
    ) {
        return ApiResponse.success(aiChatService.recommend(request, userId));
    }
}
