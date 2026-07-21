package com.bookverse.service.banner;

import com.bookverse.dto.request.banner.BannerRequestDTO;
import com.bookverse.dto.response.banner.BannerResponseDTO;

import java.util.List;

public interface BannerService {

    List<BannerResponseDTO> getPublicBanners();

    List<BannerResponseDTO> getAllBanners();

    BannerResponseDTO createBanner(BannerRequestDTO request);

    BannerResponseDTO updateBanner(Long id, BannerRequestDTO request);

    void setBannerActive(Long id, boolean active);

    void deleteBanner(Long id);
}
