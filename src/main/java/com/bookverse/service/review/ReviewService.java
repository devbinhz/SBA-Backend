package com.bookverse.service.review;

import com.bookverse.dto.request.review.ReviewRequestDTO;
import com.bookverse.dto.request.review.ReviewModerationRequestDTO;
import com.bookverse.dto.response.review.ReviewResponseDTO;
import com.bookverse.security.SecurityUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.bookverse.enums.ReviewStatus;

import java.util.Optional;

public interface ReviewService {

    ReviewResponseDTO createReview(SecurityUser securityUser, ReviewRequestDTO requestDTO);

    Page<ReviewResponseDTO> getReviewsByBook(Long bookId, Pageable pageable);

    Optional<ReviewResponseDTO> getMyReviewForBook(Long bookId, Long userId);

    Page<ReviewResponseDTO> getAllReviews(ReviewStatus status, Pageable pageable);

    ReviewResponseDTO moderateReview(Long reviewId, ReviewModerationRequestDTO request, Long adminId);

    void deleteReview(Long reviewId, SecurityUser securityUser);
}
