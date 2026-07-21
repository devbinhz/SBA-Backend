package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.dto.request.banner.BannerRequestDTO;
import com.bookverse.dto.response.banner.BannerResponseDTO;
import com.bookverse.service.banner.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
@Tag(name = "Banner", description = "Homepage banner management APIs")
public class BannerController {

    private final BannerService bannerService;

    @GetMapping
    @Operation(summary = "Get active homepage banners (Public)")
    public ApiResponse<List<BannerResponseDTO>> getBanners() {
        return ApiResponse.success(bannerService.getPublicBanners());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get all homepage banners including inactive (Admin)")
    public ApiResponse<List<BannerResponseDTO>> getBannersAdmin() {
        return ApiResponse.success(bannerService.getAllBanners());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new banner (Admin)")
    public ApiResponse<BannerResponseDTO> createBanner(@Valid @RequestBody BannerRequestDTO request) {
        return ApiResponse.success(bannerService.createBanner(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update an existing banner (Admin)")
    public ApiResponse<BannerResponseDTO> updateBanner(
            @PathVariable Long id,
            @Valid @RequestBody BannerRequestDTO request) {
        return ApiResponse.success(bannerService.updateBanner(id, request));
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Activate/deactivate a banner (Admin)")
    public ApiResponse<Void> setBannerActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        boolean active = payload.getOrDefault("active", true);
        bannerService.setBannerActive(id, active);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete a banner (Admin)")
    public ApiResponse<Void> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ApiResponse.success(null);
    }
}
