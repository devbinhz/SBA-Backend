package com.bookverse.repository;

import com.bookverse.entity.OrderStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    Page<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(Long orderId, Pageable pageable);
}
