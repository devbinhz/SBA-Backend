package com.bookverse.service.ai;

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

import java.util.List;

public interface AiChatService {
    AiChatResponse chat(AiChatRequest request, Long userId);
    AiRecommendResponse recommend(AiRecommendRequest request, Long userId);

    ChatSessionResponseDTO createSession(CreateChatSessionRequestDTO request, Long userId);
    List<ChatSessionResponseDTO> listSessions(Long userId, AiRequestType type);
    ChatSessionDetailsResponseDTO getSessionDetails(Long sessionId, Long userId);
    void deleteSession(Long sessionId, Long userId);
    ChatMessageResponseDTO sendMessage(Long sessionId, SendMessageRequestDTO request, Long userId);
}

