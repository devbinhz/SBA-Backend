package com.bookverse.repository;

import com.bookverse.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookIdAndUserId(Long bookId, Long userId);

    Page<Review> findByBookId(Long bookId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.book.id = :bookId")
    Double getAverageRatingByBookId(@Param("bookId") Long bookId);

    int countByBookId(Long bookId);
}
