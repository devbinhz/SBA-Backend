package com.bookverse.service.ai;

import com.bookverse.dto.request.ai.AiChatRequest;
import com.bookverse.dto.response.ai.AiChatResponse;
import com.bookverse.integration.rag.dto.RagIngestRequest;
import com.bookverse.integration.rag.dto.RagIngestResponse;

public interface AiChatService {
    AiChatResponse chat(AiChatRequest request);
    RagIngestResponse ingest(RagIngestRequest request);
}
