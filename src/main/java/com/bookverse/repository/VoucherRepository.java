package com.bookverse.repository;

import com.bookverse.entity.Voucher;
import com.bookverse.enums.VoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Page<Voucher> findByCampaignId(Long campaignId, Pageable pageable);
    Page<Voucher> findByStatus(VoucherStatus status, Pageable pageable);
    Page<Voucher> findByCampaignIdAndStatus(Long campaignId, VoucherStatus status, Pageable pageable);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.id = :id")
    Optional<Voucher> findWithLockById(@Param("id") Long id);
    
    @Query("SELECT v FROM Voucher v WHERE v.campaign.id = :campaignId AND v.status = :status AND v.startTime <= :now AND v.endTime >= :now AND v.claimedQuantity < v.totalQuantity")
    List<Voucher> findAvailableVouchersForCampaign(@Param("campaignId") Long campaignId, @Param("status") VoucherStatus status, @Param("now") Instant now);

    @Query("SELECT v FROM Voucher v WHERE v.status = 'ACTIVE' AND v.startTime <= :now AND v.endTime >= :now AND v.claimedQuantity < v.totalQuantity " +
           "AND (v.campaign IS NULL OR (v.campaign.isAutoDistributed = false AND v.campaign.status = 'ACTIVE' AND v.campaign.startTime <= :now AND (v.campaign.endTime IS NULL OR v.campaign.endTime >= :now)))")
    Page<Voucher> findActiveVouchers(@Param("now") Instant now, Pageable pageable);

    List<Voucher> findAllByCampaignId(Long campaignId);
}
