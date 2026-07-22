package com.bookverse.repository;

import com.bookverse.entity.RefundRequestItem;
import com.bookverse.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RefundRequestItemRepository extends JpaRepository<RefundRequestItem, Long> {

    List<RefundRequestItem> findByRefundRequestIdOrderByIdAsc(Long refundRequestId);

    @Query("""
        SELECT COUNT(i) > 0 FROM RefundRequestItem i
        WHERE i.orderItem.id = :orderItemId
          AND i.refundRequest.status NOT IN :excludedStatuses
    """)
    boolean existsActiveForOrderItem(@Param("orderItemId") Long orderItemId,
                                      @Param("excludedStatuses") Collection<RefundStatus> excludedStatuses);

    @Query("""
        SELECT COALESCE(SUM(i.quantity), 0) FROM RefundRequestItem i
        WHERE i.orderItem.id = :orderItemId
          AND i.refundRequest.status <> com.bookverse.enums.RefundStatus.REJECTED
    """)
    long sumQuantityByOrderItemIdExcludingRejected(@Param("orderItemId") Long orderItemId);
}
