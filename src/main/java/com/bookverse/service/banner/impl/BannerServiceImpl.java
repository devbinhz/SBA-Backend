package com.bookverse.service.banner.impl;

import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.banner.BannerRequestDTO;
import com.bookverse.dto.response.banner.BannerResponseDTO;
import com.bookverse.entity.Banner;
import com.bookverse.mapper.BannerMapper;
import com.bookverse.repository.BannerRepository;
import com.bookverse.service.banner.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponseDTO> getPublicBanners() {
        return bannerRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(bannerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponseDTO> getAllBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(bannerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BannerResponseDTO createBanner(BannerRequestDTO request) {
        Banner banner = bannerMapper.toEntity(request);
        Banner saved = bannerRepository.save(banner);
        return bannerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BannerResponseDTO updateBanner(Long id, BannerRequestDTO request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));
        bannerMapper.updateEntity(banner, request);
        Banner updated = bannerRepository.save(banner);
        return bannerMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void setBannerActive(Long id, boolean active) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));
        banner.setActive(active);
        bannerRepository.save(banner);
    }

    @Override
    @Transactional
    public void deleteBanner(Long id) {
        if (!bannerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Banner not found");
        }
        bannerRepository.deleteById(id);
    }
}
