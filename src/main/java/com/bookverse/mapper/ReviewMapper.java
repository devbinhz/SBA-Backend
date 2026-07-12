package com.bookverse.mapper;

import com.bookverse.dto.response.review.ReviewResponseDTO;
import com.bookverse.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponseDTO toResponse(Review review) {
        if (review == null) {
            return null;
        }

        return ReviewResponseDTO.builder()
                .id(review.getId())
                .bookId(review.getBook() != null ? review.getBook().getId() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userName(review.getUser() != null ? review.getUser().getFullName() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus())
                .moderationReason(review.getModerationReason())
                .moderatedBy(review.getModeratedBy())
                .moderatedAt(review.getModeratedAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
