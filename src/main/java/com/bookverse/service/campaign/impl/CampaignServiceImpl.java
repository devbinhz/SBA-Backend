package com.bookverse.service.campaign.impl;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.campaign.CampaignCreateRequestDTO;
import com.bookverse.dto.request.campaign.CampaignUpdateRequestDTO;
import com.bookverse.dto.response.campaign.AdminCampaignResponseDTO;
import com.bookverse.dto.response.campaign.CampaignResponseDTO;
import com.bookverse.entity.Campaign;
import com.bookverse.mapper.CampaignMapper;
import com.bookverse.repository.CampaignRepository;
import com.bookverse.service.campaign.CampaignService;
import com.bookverse.enums.CampaignStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignMapper campaignMapper;

    @Override
    @Transactional
    public AdminCampaignResponseDTO createCampaign(CampaignCreateRequestDTO request) {
        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .campaignType(request.getCampaignType())
                .isAutoDistributed(request.isAutoDistributed())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(request.getStatus())
                .build();
        
        campaign = campaignRepository.save(campaign);
        return campaignMapper.toAdminResponse(campaign);
    }

    @Override
    @Transactional
    public AdminCampaignResponseDTO updateCampaign(Long id, CampaignUpdateRequestDTO request) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        campaign.setName(request.getName());
        campaign.setCampaignType(request.getCampaignType());
        campaign.setAutoDistributed(request.isAutoDistributed());
        campaign.setStartTime(request.getStartTime());
        campaign.setEndTime(request.getEndTime());
        campaign.setStatus(request.getStatus());

        campaign = campaignRepository.save(campaign);
        return campaignMapper.toAdminResponse(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AdminCampaignResponseDTO> getAllCampaigns(Pageable pageable) {
        Page<Campaign> page = campaignRepository.findAll(pageable);
        List<AdminCampaignResponseDTO> dtos = page.getContent().stream()
                .map(campaignMapper::toAdminResponse)
                .toList();
        
        return new PageResponseDTO<>(
                dtos,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional
    public void deleteCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        campaign.setStatus(CampaignStatus.INACTIVE);
        campaignRepository.save(campaign);
    }

    @Override
    public PageResponseDTO<CampaignResponseDTO> getActiveCampaigns(Pageable pageable) {
        Page<Campaign> page = campaignRepository.findActiveCampaigns(Instant.now(), pageable);
        List<CampaignResponseDTO> dtos = page.getContent().stream()
            .map(campaignMapper::toResponse)
            .toList();
        return new PageResponseDTO<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
