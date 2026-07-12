package com.bookverse.repository;

import com.bookverse.entity.Review;
import com.bookverse.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookIdAndUserId(Long bookId, Long userId);

    Optional<Review> findByBookIdAndUserId(Long bookId, Long userId);

    Page<Review> findByBookIdAndStatus(Long bookId, ReviewStatus status, Pageable pageable);

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.book.id = :bookId AND r.status = com.bookverse.enums.ReviewStatus.PUBLISHED")
    Double getPublishedAverageRatingByBookId(@Param("bookId") Long bookId);

    int countByBookIdAndStatus(Long bookId, ReviewStatus status);
}
