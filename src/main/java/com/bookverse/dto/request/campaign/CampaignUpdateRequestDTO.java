package com.bookverse.dto.request.campaign;

import com.bookverse.enums.CampaignStatus;
import com.bookverse.enums.CampaignType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.Instant;

@Data
public class CampaignUpdateRequestDTO {
    
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Campaign type is required")
    private CampaignType campaignType;

    private boolean isAutoDistributed;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    private Instant endTime;

    @NotNull(message = "Status is required")
    private CampaignStatus status;
}
