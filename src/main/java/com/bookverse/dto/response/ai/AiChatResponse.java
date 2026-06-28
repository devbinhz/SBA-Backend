package com.bookverse.dto.response.ai;

import java.util.List;

public record AiChatResponse(
        String answer,
        List<ChatSource> sources
) {}
