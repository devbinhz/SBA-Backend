package com.bookverse.mapper;

import com.bookverse.dto.response.campaign.AdminCampaignResponseDTO;
import com.bookverse.dto.response.campaign.CampaignResponseDTO;
import com.bookverse.entity.Campaign;
import org.springframework.stereotype.Component;

@Component
public class CampaignMapper {

    public AdminCampaignResponseDTO toAdminResponse(Campaign campaign) {
        if (campaign == null) {
            return null;
        }

        return AdminCampaignResponseDTO.builder()
            .id(campaign.getId())
            .name(campaign.getName())
            .campaignType(campaign.getCampaignType())
            .isAutoDistributed(campaign.isAutoDistributed())
            .startTime(campaign.getStartTime())
            .endTime(campaign.getEndTime())
            .status(campaign.getStatus())
            .createdAt(campaign.getCreatedAt())
            .updatedAt(campaign.getUpdatedAt())
            .build();
    }

    public CampaignResponseDTO toResponse(Campaign campaign) {
        if (campaign == null) {
            return null;
        }

        return CampaignResponseDTO.builder()
            .id(campaign.getId())
            .name(campaign.getName())
            .campaignType(campaign.getCampaignType())
            .startTime(campaign.getStartTime())
            .endTime(campaign.getEndTime())
            .build();
    }
}
