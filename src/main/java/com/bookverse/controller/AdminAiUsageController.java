package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.response.ai.AiUsageLogResponseDTO;
import com.bookverse.dto.response.ai.DailyAiUsageSummaryResponseDTO;
import com.bookverse.dto.response.ai.UserAiUsageSummaryResponseDTO;
import com.bookverse.enums.AiRequestType;
import com.bookverse.service.ai.AiUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/ai/usage")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "AI Usage", description = "Admin endpoints to monitor and manage AI usage statistics")
public class AdminAiUsageController {

    private final AiUsageService aiUsageService;

    @GetMapping("/logs")
    @Operation(summary = "Get paginated AI usage logs")
    public ApiResponse<PageResponseDTO<AiUsageLogResponseDTO>> getLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AiRequestType requestType,
            @ParameterObject Pageable pageable
    ) {
        return ApiResponse.success(aiUsageService.getLogs(userId, requestType, pageable));
    }

    @GetMapping("/summary/users")
    @Operation(summary = "Get AI usage summary grouped by users")
    public ApiResponse<List<UserAiUsageSummaryResponseDTO>> getUserSummary() {
        return ApiResponse.success(aiUsageService.getUserSummary());
    }

    @GetMapping("/summary/daily")
    @Operation(summary = "Get daily AI usage summary")
    public ApiResponse<List<DailyAiUsageSummaryResponseDTO>> getDailySummary() {
        return ApiResponse.success(aiUsageService.getDailySummary());
    }
}
