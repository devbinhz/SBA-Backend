package com.bookverse.service.ai.impl;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.response.ai.AiUsageLogResponseDTO;
import com.bookverse.dto.response.ai.DailyAiUsageSummaryResponseDTO;
import com.bookverse.dto.response.ai.UserAiUsageSummaryResponseDTO;
import com.bookverse.entity.AiUsageLog;
import com.bookverse.entity.User;
import com.bookverse.enums.AiRequestType;
import com.bookverse.repository.AiUsageLogRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.service.ai.AiUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiUsageServiceImpl implements AiUsageService {

    private final AiUsageLogRepository aiUsageLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void logUsage(Long userId, AiRequestType requestType, String query, String response, int promptTokens, int completionTokens, long durationMs) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        AiUsageLog log = AiUsageLog.builder()
                .user(user)
                .requestType(requestType)
                .query(query)
                .response(response)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .durationMs(durationMs)
                .build();
        aiUsageLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AiUsageLogResponseDTO> getLogs(Long userId, AiRequestType requestType, Pageable pageable) {
        Page<AiUsageLog> page = aiUsageLogRepository.findLogs(userId, requestType, pageable);
        List<AiUsageLogResponseDTO> content = page.getContent().stream()
                .map(log -> new AiUsageLogResponseDTO(
                        log.getId(),
                        log.getUser().getId(),
                        log.getUser().getEmail(),
                        log.getUser().getFullName(),
                        log.getRequestType(),
                        log.getQuery(),
                        log.getResponse(),
                        log.getPromptTokens(),
                        log.getCompletionTokens(),
                        log.getTotalTokens(),
                        log.getDurationMs(),
                        log.getCreatedAt()
                ))
                .toList();
        return new PageResponseDTO<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAiUsageSummaryResponseDTO> getUserSummary() {
        return aiUsageLogRepository.getUserUsageSummary().stream()
                .map(p -> new UserAiUsageSummaryResponseDTO(
                        p.getUserId(),
                        p.getFullName(),
                        p.getEmail(),
                        p.getRequestCount(),
                        p.getTotalPromptTokens() != null ? p.getTotalPromptTokens() : 0L,
                        p.getTotalCompletionTokens() != null ? p.getTotalCompletionTokens() : 0L,
                        p.getTotalTokens() != null ? p.getTotalTokens() : 0L,
                        p.getTotalDurationMs() != null ? p.getTotalDurationMs() : 0L,
                        p.getLastUsedAt()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyAiUsageSummaryResponseDTO> getDailySummary() {
        return aiUsageLogRepository.getDailyUsageSummary().stream()
                .map(p -> new DailyAiUsageSummaryResponseDTO(
                        p.getDate().toLocalDate(),
                        p.getRequestCount(),
                        p.getTotalTokens() != null ? p.getTotalTokens() : 0L
                ))
                .toList();
    }
}
