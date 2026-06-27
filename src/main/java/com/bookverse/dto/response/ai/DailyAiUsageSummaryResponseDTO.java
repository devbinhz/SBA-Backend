package com.bookverse.dto.response.ai;

import java.time.LocalDate;

public record DailyAiUsageSummaryResponseDTO(
        LocalDate date,
        Long requestCount,
        Long totalTokens
) {}
