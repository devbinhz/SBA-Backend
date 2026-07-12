package com.bookverse.service.review.impl;

import com.bookverse.common.exception.ConflictException;
import com.bookverse.common.exception.ForbiddenException;
import com.bookverse.dto.request.review.ReviewRequestDTO;
import com.bookverse.dto.request.review.ReviewModerationRequestDTO;
import com.bookverse.dto.response.review.ReviewResponseDTO;
import com.bookverse.entity.Book;
import com.bookverse.entity.Review;
import com.bookverse.entity.User;
import com.bookverse.mapper.ReviewMapper;
import com.bookverse.enums.ReviewStatus;
import com.bookverse.repository.BookRepository;
import com.bookverse.repository.OrderItemRepository;
import com.bookverse.repository.ReviewRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User user;
    private SecurityUser securityUser;
    private Book book;
    private ReviewRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).enabled(true).build();
        securityUser = new SecurityUser(user);
        book = Book.builder().id(10L).active(true).ratingAvg(BigDecimal.ZERO).reviewCount(0).build();
        requestDTO = ReviewRequestDTO.builder().bookId(10L).rating(5).comment("Great!").build();
    }

    @Test
    void createReview_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reviewRepository.existsByBookIdAndUserId(10L, 1L)).thenReturn(false);
        when(orderItemRepository.existsDeliveredOrderForUserAndBook(1L, 10L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
            Review r = i.getArgument(0);
            r.setId(100L);
            return r;
        });
        when(reviewRepository.getPublishedAverageRatingByBookId(10L)).thenReturn(5.0);
        when(reviewRepository.countByBookIdAndStatus(10L, ReviewStatus.PUBLISHED)).thenReturn(1);

        ReviewResponseDTO mockResponse = ReviewResponseDTO.builder().id(100L).rating(5).build();
        when(reviewMapper.toResponse(any(Review.class))).thenReturn(mockResponse);

        ReviewResponseDTO response = reviewService.createReview(securityUser, requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);

        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(bookCaptor.capture());
        Book savedBook = bookCaptor.getValue();
        assertThat(savedBook.getRatingAvg()).isEqualByComparingTo(BigDecimal.valueOf(5.0));
        assertThat(savedBook.getReviewCount()).isEqualTo(1);
    }

    @Test
    void createReview_whenOrderNotDelivered_throwsForbidden() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reviewRepository.existsByBookIdAndUserId(10L, 1L)).thenReturn(false);
        when(orderItemRepository.existsDeliveredOrderForUserAndBook(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(securityUser, requestDTO))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("received it");
    }

    @Test
    void createReview_whenReviewDuplicate_throwsConflict() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reviewRepository.existsByBookIdAndUserId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(securityUser, requestDTO))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    void deleteReview_byOwner_success() {
        Review review = Review.builder().id(100L).user(user).book(book).build();
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(reviewRepository.getPublishedAverageRatingByBookId(10L)).thenReturn(0.0);
        when(reviewRepository.countByBookIdAndStatus(10L, ReviewStatus.PUBLISHED)).thenReturn(0);

        reviewService.deleteReview(100L, securityUser);

        verify(reviewRepository).delete(review);
        verify(bookRepository).save(book);
    }

    @Test
    void moderateReview_hidesReviewAndRecalculatesPublishedRating() {
        Review review = Review.builder()
                .id(100L)
                .user(user)
                .book(book)
                .status(ReviewStatus.PUBLISHED)
                .build();
        ReviewModerationRequestDTO request = new ReviewModerationRequestDTO();
        request.setStatus(ReviewStatus.HIDDEN);
        request.setReason("Contains inappropriate content");
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewMapper.toResponse(review)).thenReturn(ReviewResponseDTO.builder()
                .id(100L)
                .status(ReviewStatus.HIDDEN)
                .build());
        when(reviewRepository.getPublishedAverageRatingByBookId(10L)).thenReturn(0.0);
        when(reviewRepository.countByBookIdAndStatus(10L, ReviewStatus.PUBLISHED)).thenReturn(0);

        ReviewResponseDTO response = reviewService.moderateReview(100L, request, 9L);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(review.getModerationReason()).isEqualTo("Contains inappropriate content");
        assertThat(review.getModeratedBy()).isEqualTo(9L);
        assertThat(review.getModeratedAt()).isNotNull();
        verify(bookRepository).save(book);
    }

    @Test
    void getReviewSummaryReturnsCountsForEveryRating() {
        book.setRatingAvg(BigDecimal.valueOf(4.5));
        book.setReviewCount(3);
        ReviewRepository.RatingCountProjection fiveStars = mock(ReviewRepository.RatingCountProjection.class);
        ReviewRepository.RatingCountProjection fourStars = mock(ReviewRepository.RatingCountProjection.class);
        when(fiveStars.getRating()).thenReturn(5);
        when(fiveStars.getCount()).thenReturn(2L);
        when(fourStars.getRating()).thenReturn(4);
        when(fourStars.getCount()).thenReturn(1L);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reviewRepository.countPublishedReviewsByRating(10L)).thenReturn(List.of(fiveStars, fourStars));

        var summary = reviewService.getReviewSummary(10L);

        assertThat(summary.getAverageRating()).isEqualByComparingTo("4.5");
        assertThat(summary.getTotalReviews()).isEqualTo(3);
        assertThat(summary.getRatingCounts()).containsAllEntriesOf(Map.of(
                5, 2L,
                4, 1L,
                3, 0L,
                2, 0L,
                1, 0L
        ));
    }
}
