package com.bookverse.service.ai;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.response.ai.AiUsageLogResponseDTO;
import com.bookverse.dto.response.ai.DailyAiUsageSummaryResponseDTO;
import com.bookverse.dto.response.ai.UserAiUsageSummaryResponseDTO;
import com.bookverse.enums.AiRequestType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AiUsageService {

    void logUsage(Long userId, AiRequestType requestType, String query, String response, int promptTokens, int completionTokens, long durationMs);

    PageResponseDTO<AiUsageLogResponseDTO> getLogs(Long userId, AiRequestType requestType, Pageable pageable);

    List<UserAiUsageSummaryResponseDTO> getUserSummary();

    List<DailyAiUsageSummaryResponseDTO> getDailySummary();
}
