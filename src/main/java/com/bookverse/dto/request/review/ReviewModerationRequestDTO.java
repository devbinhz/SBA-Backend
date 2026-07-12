package com.bookverse.dto.request.review;

import com.bookverse.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewModerationRequestDTO {

    @NotNull(message = "Review status is required")
    private ReviewStatus status;

    @Size(max = 500, message = "Moderation reason must be at most 500 characters")
    private String reason;
}
