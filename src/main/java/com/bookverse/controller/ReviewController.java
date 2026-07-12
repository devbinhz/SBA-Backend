package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.review.ReviewRequestDTO;
import com.bookverse.dto.request.review.ReviewModerationRequestDTO;
import com.bookverse.dto.response.review.ReviewResponseDTO;
import com.bookverse.dto.response.review.ReviewModerationHistoryResponseDTO;
import com.bookverse.dto.response.review.ReviewSummaryResponseDTO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.bookverse.enums.ReviewStatus;
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
    public ApiResponse<PageResponseDTO<ReviewResponseDTO>> getReviewsByBook(
            @PathVariable Long bookId,
            Pageable pageable) {
        return ApiResponse.success(PageResponseDTO.from(reviewService.getReviewsByBook(bookId, pageable)));
    }

    @GetMapping("/books/{bookId}/reviews/summary")
    @Operation(summary = "Get published review rating breakdown for a book (Public)")
    public ApiResponse<ReviewSummaryResponseDTO> getReviewSummary(@PathVariable Long bookId) {
        return ApiResponse.success(reviewService.getReviewSummary(bookId));
    }

    @GetMapping("/books/{bookId}/reviews/me")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @Operation(summary = "Get the current customer's review for a book")
    public ApiResponse<ReviewResponseDTO> getMyReviewForBook(
            @PathVariable Long bookId,
            @AuthenticationPrincipal(expression = "user.id") Long userId) {
        return ApiResponse.success(reviewService.getMyReviewForBook(bookId, userId).orElse(null));
    }

    @GetMapping("/admin/reviews")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get all reviews (Admin)")
    public ApiResponse<PageResponseDTO<ReviewResponseDTO>> getAllReviews(
            @RequestParam(required = false) ReviewStatus status,
            Pageable pageable) {
        return ApiResponse.success(PageResponseDTO.from(reviewService.getAllReviews(status, pageable)));
    }

    @PutMapping("/admin/reviews/{reviewId}/moderation")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Hide or restore a review (Admin)")
    public ApiResponse<ReviewResponseDTO> moderateReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "user.id") Long adminId,
            @Valid @RequestBody ReviewModerationRequestDTO request) {
        return ApiResponse.success(reviewService.moderateReview(reviewId, request, adminId));
    }

    @GetMapping("/admin/reviews/{reviewId}/moderation-history")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get review moderation history (Admin)")
    public ApiResponse<PageResponseDTO<ReviewModerationHistoryResponseDTO>> getModerationHistory(
            @PathVariable Long reviewId,
            Pageable pageable) {
        return ApiResponse.success(PageResponseDTO.from(reviewService.getModerationHistory(reviewId, pageable)));
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
