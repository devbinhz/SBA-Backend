package com.bookverse.service.review.impl;

import com.bookverse.common.exception.ConflictException;
import com.bookverse.common.exception.ForbiddenException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.review.ReviewRequestDTO;
import com.bookverse.dto.response.review.ReviewResponseDTO;
import com.bookverse.entity.Book;
import com.bookverse.entity.Review;
import com.bookverse.entity.User;
import com.bookverse.enums.UserRole;
import com.bookverse.mapper.ReviewMapper;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.OrderItemRepository;
import com.bookverse.repository.ReviewRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.security.SecurityUser;
import com.bookverse.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewResponseDTO createReview(SecurityUser securityUser, ReviewRequestDTO requestDTO) {
        User user = userRepository.findById(securityUser.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new ForbiddenException("Account is disabled");
        }

        Book book = bookRepository.findById(requestDTO.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (!book.isActive()) {
            throw new ForbiddenException("Book is inactive");
        }

        if (reviewRepository.existsByBookIdAndUserId(book.getId(), user.getId())) {
            throw new ConflictException("You have already reviewed this book.", "DUPLICATE_RESOURCE");
        }

        if (!orderItemRepository.existsDeliveredOrderForUserAndBook(user.getId(), book.getId())) {
            throw new ForbiddenException("You can only review a book after you have received it (order delivered).");
        }

        Review review = Review.builder()
                .book(book)
                .user(user)
                .rating(requestDTO.getRating())
                .comment(requestDTO.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);
        recalculateBookRating(book);

        return reviewMapper.toResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getReviewsByBook(Long bookId, Pageable pageable) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book not found");
        }
        return reviewRepository.findByBookId(bookId, pageable)
                .map(reviewMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getAllReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable)
                .map(reviewMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, SecurityUser securityUser) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        boolean isAdmin = securityUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !review.getUser().getId().equals(securityUser.getUser().getId())) {
            throw new ForbiddenException("You do not have permission to delete this review.");
        }

        Book book = review.getBook();
        reviewRepository.delete(review);
        recalculateBookRating(book);
    }

    private void recalculateBookRating(Book book) {
        Double avg = reviewRepository.getAverageRatingByBookId(book.getId());
        int count = reviewRepository.countByBookId(book.getId());

        book.setRatingAvg(BigDecimal.valueOf(avg != null ? avg : 0.0));
        book.setReviewCount(count);
        bookRepository.save(book);
    }
}
