package com.bookverse.mapper;

import com.bookverse.config.MinioProperties;
import com.bookverse.dto.request.giftwrap.GiftWrapRequestDTO;
import com.bookverse.dto.response.giftwrap.GiftWrapResponseDTO;
import com.bookverse.entity.GiftWrap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GiftWrapMapper {

    private final MinioProperties minioProperties;

    public GiftWrap toEntity(GiftWrapRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        GiftWrap giftWrap = new GiftWrap();
        giftWrap.setName(dto.getName());
        giftWrap.setImageKey(dto.getImageKey());
        giftWrap.setFeeVnd(dto.getFeeVnd());
        giftWrap.setDisplayOrder(dto.getDisplayOrder());
        giftWrap.setActive(dto.isActive());
        return giftWrap;
    }

    public GiftWrapResponseDTO toResponse(GiftWrap entity) {
        if (entity == null) {
            return null;
        }
        String imageUrl = null;
        if (entity.getImageKey() != null && !entity.getImageKey().isBlank()) {
            imageUrl = minioProperties.publicEndpoint() + "/" + minioProperties.thumbnailsBucket() + "/" + entity.getImageKey();
        }
        return GiftWrapResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .imageUrl(imageUrl)
                .imageKey(entity.getImageKey())
                .feeVnd(entity.getFeeVnd())
                .displayOrder(entity.getDisplayOrder())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(GiftWrap entity, GiftWrapRequestDTO dto) {
        if (dto == null || entity == null) {
            return;
        }
        entity.setName(dto.getName());
        entity.setImageKey(dto.getImageKey());
        entity.setFeeVnd(dto.getFeeVnd());
        entity.setDisplayOrder(dto.getDisplayOrder());
        entity.setActive(dto.isActive());
    }
}
