package com.bookverse.service.ai;

import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.request.ai.AiRecommendRequest;
import com.bookverse.dto.response.ai.AiChatResponse;
import com.bookverse.dto.response.ai.AiRecommendResponse;

public interface AiChatService {
    AiChatResponse chat(AiChatRequest request, Long userId);
    AiRecommendResponse recommend(AiRecommendRequest request, Long userId);
}

