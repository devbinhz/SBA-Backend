package com.bookverse.service.ai;

import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.response.ai.AiChatResponse;

public interface AiChatService {
    AiChatResponse chat(AiChatRequest request, Long userId);
}
