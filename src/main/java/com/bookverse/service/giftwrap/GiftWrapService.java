package com.bookverse.service.giftwrap;

import com.bookverse.dto.request.giftwrap.GiftWrapRequestDTO;
import com.bookverse.dto.response.giftwrap.GiftWrapResponseDTO;
import com.bookverse.entity.GiftWrap;

import java.util.List;

public interface GiftWrapService {

    List<GiftWrapResponseDTO> getPublicGiftWraps();

    List<GiftWrapResponseDTO> getAllGiftWraps();

    GiftWrapResponseDTO createGiftWrap(GiftWrapRequestDTO request);

    GiftWrapResponseDTO updateGiftWrap(Long id, GiftWrapRequestDTO request);

    void setGiftWrapActive(Long id, boolean active);

    void deleteGiftWrap(Long id);

    GiftWrap getActiveGiftWrapOrThrow(Long id);
}
