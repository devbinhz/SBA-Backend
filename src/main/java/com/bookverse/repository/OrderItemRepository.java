package com.bookverse.repository;

import com.bookverse.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    @Query("""
        SELECT COUNT(DISTINCT oi.book.id) FROM OrderItem oi
        JOIN oi.order o
        WHERE o.user.id = :userId
          AND oi.book.id IN :bookIds
          AND o.status IN ('PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED')
    """)
    long countPurchasedByUserAndBooks(@Param("userId") Long userId, @Param("bookIds") List<Long> bookIds);
}
