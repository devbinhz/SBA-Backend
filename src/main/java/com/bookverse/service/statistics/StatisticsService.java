package com.bookverse.service.statistics;

import com.bookverse.dto.response.statistics.StatisticsOverviewResponseDTO;

public interface StatisticsService {
    StatisticsOverviewResponseDTO getOverview();
}
