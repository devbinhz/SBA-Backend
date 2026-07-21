package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.campaign.CampaignCreateRequestDTO;
import com.bookverse.dto.request.campaign.CampaignUpdateRequestDTO;
import com.bookverse.dto.response.campaign.AdminCampaignResponseDTO;
import com.bookverse.service.campaign.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/campaigns")
@RequiredArgsConstructor
@Tag(name = "Admin Campaign", description = "Admin Campaign APIs")
public class AdminCampaignController {

    private final CampaignService campaignService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create a campaign")
    public ApiResponse<AdminCampaignResponseDTO> createCampaign(@Valid @RequestBody CampaignCreateRequestDTO request) {
        return ApiResponse.success(campaignService.createCampaign(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update a campaign")
    public ApiResponse<AdminCampaignResponseDTO> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody CampaignUpdateRequestDTO request) {
        return ApiResponse.success(campaignService.updateCampaign(id, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "List all campaigns")
    public ApiResponse<PageResponseDTO<AdminCampaignResponseDTO>> getCampaigns(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(campaignService.getAllCampaigns(pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Deactivate a campaign (Soft Delete)")
    public ApiResponse<Void> deleteCampaign(@PathVariable Long id) {
        campaignService.deleteCampaign(id);
        return ApiResponse.success(null);
    }
}
