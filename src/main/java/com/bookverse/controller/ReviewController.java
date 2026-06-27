package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.dto.request.review.ReviewRequestDTO;
import com.bookverse.dto.response.review.ReviewResponseDTO;
import com.bookverse.security.SecurityUser;
import com.bookverse.service.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Review", description = "Review Management APIs")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/books/{bookId}/reviews")
    @Operation(summary = "Get reviews for a book (Public)")
    public ApiResponse<Page<ReviewResponseDTO>> getReviewsByBook(
            @PathVariable Long bookId,
            Pageable pageable) {
        return ApiResponse.success(reviewService.getReviewsByBook(bookId, pageable));
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new review (Customer)")
    public ApiResponse<ReviewResponseDTO> createReview(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ReviewRequestDTO request) {
        return ApiResponse.success(reviewService.createReview(securityUser, request));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a review (Customer/Admin)")
    public ApiResponse<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal SecurityUser securityUser) {
        reviewService.deleteReview(reviewId, securityUser);
        return ApiResponse.success(null);
    }
}
