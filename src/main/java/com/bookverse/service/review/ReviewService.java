package com.bookverse.service.review;

import com.bookverse.dto.request.review.ReviewRequestDTO;
import com.bookverse.dto.response.review.ReviewResponseDTO;
import com.bookverse.security.SecurityUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponseDTO createReview(SecurityUser securityUser, ReviewRequestDTO requestDTO);

    Page<ReviewResponseDTO> getReviewsByBook(Long bookId, Pageable pageable);

    void deleteReview(Long reviewId, SecurityUser securityUser);
}
