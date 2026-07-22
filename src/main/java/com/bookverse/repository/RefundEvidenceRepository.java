package com.bookverse.repository;

import com.bookverse.entity.RefundEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundEvidenceRepository extends JpaRepository<RefundEvidence, Long> {

    List<RefundEvidence> findByRefundRequestIdOrderByIdAsc(Long refundRequestId);

    long countByRefundRequestId(Long refundRequestId);
}
