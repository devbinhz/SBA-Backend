package com.bookverse.repository;

import com.bookverse.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.bookverse.enums.CampaignType;
import com.bookverse.enums.CampaignStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    Page<Campaign> findAll(Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.campaignType = :type AND c.status = :status AND c.isAutoDistributed = true AND c.startTime <= :now AND (c.endTime IS NULL OR c.endTime >= :now)")
    List<Campaign> findActiveAutoDistributedCampaigns(@Param("type") CampaignType type, @Param("status") CampaignStatus status, @Param("now") Instant now);

    @Query("SELECT c FROM Campaign c WHERE c.status = 'ACTIVE' AND c.startTime <= :now AND (c.endTime IS NULL OR c.endTime >= :now)")
    Page<Campaign> findActiveCampaigns(@Param("now") Instant now, Pageable pageable);
}
