package com.bookverse.dto.response.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsOverviewResponseDTO {
    private long totalUsers;
    private long totalBooks;
    private long activeBooks;
    private long totalOrders;
    private long recognizedRevenue;
    private List<BookSellingStatsDTO> topSellingBooks;
}
