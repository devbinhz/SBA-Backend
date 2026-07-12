package com.bookverse.repository;

import com.bookverse.entity.ReviewModerationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewModerationHistoryRepository extends JpaRepository<ReviewModerationHistory, Long> {

    Page<ReviewModerationHistory> findByReviewId(Long reviewId, Pageable pageable);
}
