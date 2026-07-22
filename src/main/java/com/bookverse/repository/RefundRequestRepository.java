package com.bookverse.repository;

import com.bookverse.entity.RefundRequest;
import com.bookverse.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long>, JpaSpecificationExecutor<RefundRequest> {

    Optional<RefundRequest> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    boolean existsByOrderIdAndStatusIn(Long orderId, Collection<RefundStatus> statuses);
}
