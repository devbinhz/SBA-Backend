package com.bookverse.mapper;

import com.bookverse.config.MinioProperties;
import com.bookverse.dto.request.banner.BannerRequestDTO;
import com.bookverse.dto.response.banner.BannerResponseDTO;
import com.bookverse.entity.Banner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BannerMapper {

    private final MinioProperties minioProperties;

    public Banner toEntity(BannerRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Banner banner = new Banner();
        banner.setTitle(dto.getTitle());
        banner.setSubtitle(dto.getSubtitle());
        banner.setImageKey(dto.getImageKey());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setDisplayOrder(dto.getDisplayOrder());
        banner.setActive(dto.isActive());
        return banner;
    }

    public BannerResponseDTO toResponse(Banner entity) {
        if (entity == null) {
            return null;
        }
        String imageUrl = null;
        if (entity.getImageKey() != null && !entity.getImageKey().isBlank()) {
            imageUrl = minioProperties.publicEndpoint() + "/" + minioProperties.thumbnailsBucket() + "/" + entity.getImageKey();
        }
        return BannerResponseDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .subtitle(entity.getSubtitle())
                .imageUrl(imageUrl)
                .imageKey(entity.getImageKey())
                .linkUrl(entity.getLinkUrl())
                .displayOrder(entity.getDisplayOrder())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(Banner entity, BannerRequestDTO dto) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setTitle(dto.getTitle());
        entity.setSubtitle(dto.getSubtitle());
        entity.setImageKey(dto.getImageKey());
        entity.setLinkUrl(dto.getLinkUrl());
        entity.setDisplayOrder(dto.getDisplayOrder());
        entity.setActive(dto.isActive());
    }
}
