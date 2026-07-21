package com.bookverse.service.campaign;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.campaign.CampaignCreateRequestDTO;
import com.bookverse.dto.request.campaign.CampaignUpdateRequestDTO;
import com.bookverse.dto.response.campaign.AdminCampaignResponseDTO;
import org.springframework.data.domain.Pageable;

import com.bookverse.dto.response.campaign.CampaignResponseDTO;

public interface CampaignService {
    AdminCampaignResponseDTO createCampaign(CampaignCreateRequestDTO request);
    AdminCampaignResponseDTO updateCampaign(Long id, CampaignUpdateRequestDTO request);
    PageResponseDTO<AdminCampaignResponseDTO> getAllCampaigns(Pageable pageable);
    void deleteCampaign(Long id);
    
    // Public API
    PageResponseDTO<CampaignResponseDTO> getActiveCampaigns(Pageable pageable);
}
