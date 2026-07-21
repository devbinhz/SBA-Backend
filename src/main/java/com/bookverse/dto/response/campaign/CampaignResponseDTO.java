package com.bookverse.dto.response.campaign;

import com.bookverse.enums.CampaignType;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class CampaignResponseDTO {
    private Long id;
    private String name;
    private CampaignType campaignType;
    private Instant startTime;
    private Instant endTime;
}
