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
public class ReviewResponseDTO {

    private Long id;
    private Long bookId;
    private Long userId;
    private String userName;
    private Integer rating;
    private String comment;
    private ReviewStatus status;
    private String moderationReason;
    private Long moderatedBy;
    private Instant moderatedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
