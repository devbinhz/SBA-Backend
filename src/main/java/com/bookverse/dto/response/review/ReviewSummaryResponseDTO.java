package com.bookverse.dto.response.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryResponseDTO {
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Map<Integer, Long> ratingCounts;
}
