package com.bookverse.dto.response.review;

import com.bookverse.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewModerationHistoryResponseDTO {
    private Long id;
    private Long reviewId;
    private ReviewStatus fromStatus;
    private ReviewStatus toStatus;
    private String reason;
    private Long moderatedBy;
    private String moderatorName;
    private Instant createdAt;
}
