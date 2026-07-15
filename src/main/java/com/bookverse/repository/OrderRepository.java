package com.bookverse.repository;

import com.bookverse.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status IN :statuses")
    long sumRevenueByStatuses(@Param("statuses") List<com.bookverse.enums.OrderStatus> statuses);

    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<Order> findByGuestEmailAndIdempotencyKey(String guestEmail, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findWithLockById(@Param("id") Long id);

    @Query("""
            select o.id
            from Order o
            where o.status = com.bookverse.enums.OrderStatus.PENDING_PAYMENT
              and o.expiresAt is not null
              and o.expiresAt <= :now
            order by o.expiresAt asc, o.id asc
            """)
    List<Long> findExpiredPendingOrderIds(@Param("now") Instant now, Pageable pageable);
}
