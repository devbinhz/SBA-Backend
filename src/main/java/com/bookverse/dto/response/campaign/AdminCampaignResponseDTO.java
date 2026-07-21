package com.bookverse.dto.response.campaign;

import com.bookverse.enums.CampaignStatus;
import com.bookverse.enums.CampaignType;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class AdminCampaignResponseDTO {
    private Long id;
    private String name;
    private CampaignType campaignType;
    private boolean isAutoDistributed;
    private Instant startTime;
    private Instant endTime;
    private CampaignStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
