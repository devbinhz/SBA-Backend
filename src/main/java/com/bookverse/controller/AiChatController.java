package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.request.ai.AiRecommendRequest;
import com.bookverse.dto.request.ai.CreateChatSessionRequestDTO;
import com.bookverse.dto.request.ai.SendMessageRequestDTO;
import com.bookverse.dto.response.ai.AiChatResponse;
import com.bookverse.dto.response.ai.AiRecommendResponse;
import com.bookverse.dto.response.ai.ChatMessageResponseDTO;
import com.bookverse.dto.response.ai.ChatSessionDetailsResponseDTO;
import com.bookverse.dto.response.ai.ChatSessionResponseDTO;
import com.bookverse.enums.AiRequestType;
import com.bookverse.service.ai.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PostMapping("/chat/sessions")
    @Operation(summary = "Create a new chat session")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<ChatSessionResponseDTO> createSession(
            @Valid @RequestBody CreateChatSessionRequestDTO request,
            @AuthenticationPrincipal(expression = "user.id") Long userId
    ) {
        return ApiResponse.success(aiChatService.createSession(request, userId));
    }

    @GetMapping("/chat/sessions")
    @Operation(summary = "List user chat sessions")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<ChatSessionResponseDTO>> listSessions(
            @RequestParam AiRequestType type,
            @AuthenticationPrincipal(expression = "user.id") Long userId
    ) {
        return ApiResponse.success(aiChatService.listSessions(userId, type));
    }

    @GetMapping("/chat/sessions/{sessionId}")
    @Operation(summary = "Get chat session details and message history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<ChatSessionDetailsResponseDTO> getSessionDetails(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal(expression = "user.id") Long userId
    ) {
        return ApiResponse.success(aiChatService.getSessionDetails(sessionId, userId));
    }

    @DeleteMapping("/chat/sessions/{sessionId}")
    @Operation(summary = "Delete a chat session")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<Void> deleteSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal(expression = "user.id") Long userId
    ) {
        aiChatService.deleteSession(sessionId, userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/chat/sessions/{sessionId}/messages")
    @Operation(summary = "Send a message within a chat session")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<ChatMessageResponseDTO> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequestDTO request,
            @AuthenticationPrincipal(expression = "user.id") Long userId
    ) {
        return ApiResponse.success(aiChatService.sendMessage(sessionId, request, userId));
    }
}

