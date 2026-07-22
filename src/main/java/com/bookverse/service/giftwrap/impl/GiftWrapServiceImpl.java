package com.bookverse.service.giftwrap.impl;

import com.bookverse.common.exception.ConflictException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.giftwrap.GiftWrapRequestDTO;
import com.bookverse.dto.response.giftwrap.GiftWrapResponseDTO;
import com.bookverse.entity.GiftWrap;
import com.bookverse.mapper.GiftWrapMapper;
import com.bookverse.repository.GiftWrapRepository;
import com.bookverse.repository.OrderRepository;
import com.bookverse.service.giftwrap.GiftWrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GiftWrapServiceImpl implements GiftWrapService {

    private final GiftWrapRepository giftWrapRepository;
    private final OrderRepository orderRepository;
    private final GiftWrapMapper giftWrapMapper;

    @Override
    @Transactional(readOnly = true)
    public List<GiftWrapResponseDTO> getPublicGiftWraps() {
        return giftWrapRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(giftWrapMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GiftWrapResponseDTO> getAllGiftWraps() {
        return giftWrapRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(giftWrapMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public GiftWrapResponseDTO createGiftWrap(GiftWrapRequestDTO request) {
        GiftWrap giftWrap = giftWrapMapper.toEntity(request);
        GiftWrap saved = giftWrapRepository.save(giftWrap);
        return giftWrapMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public GiftWrapResponseDTO updateGiftWrap(Long id, GiftWrapRequestDTO request) {
        GiftWrap giftWrap = giftWrapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gift wrap not found"));
        giftWrapMapper.updateEntity(giftWrap, request);
        GiftWrap updated = giftWrapRepository.save(giftWrap);
        return giftWrapMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void setGiftWrapActive(Long id, boolean active) {
        GiftWrap giftWrap = giftWrapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gift wrap not found"));
        giftWrap.setActive(active);
        giftWrapRepository.save(giftWrap);
    }

    @Override
    @Transactional
    public void deleteGiftWrap(Long id) {
        if (!giftWrapRepository.existsById(id)) {
            throw new ResourceNotFoundException("Gift wrap not found");
        }
        if (orderRepository.existsByGiftWrapId(id)) {
            throw new ConflictException("Cannot delete a gift wrap that has been used in orders");
        }
        giftWrapRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public GiftWrap getActiveGiftWrapOrThrow(Long id) {
        GiftWrap giftWrap = giftWrapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gift wrap not found"));
        if (!giftWrap.isActive()) {
            throw new ResourceNotFoundException("Gift wrap not found");
        }
        return giftWrap;
    }
}
