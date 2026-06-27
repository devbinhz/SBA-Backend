package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.dto.response.statistics.StatisticsOverviewResponseDTO;
import com.bookverse.service.statistics.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Admin statistics API")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "Get statistics overview")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/overview")
    public ApiResponse<StatisticsOverviewResponseDTO> getOverview() {
        return ApiResponse.success(statisticsService.getOverview());
    }
}
