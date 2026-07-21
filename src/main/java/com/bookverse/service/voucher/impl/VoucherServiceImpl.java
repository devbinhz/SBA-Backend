package com.bookverse.service.voucher.impl;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.common.exception.BadRequestException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.dto.request.voucher.VoucherCreateRequestDTO;
import com.bookverse.dto.request.voucher.VoucherUpdateRequestDTO;
import com.bookverse.dto.response.voucher.AdminVoucherResponseDTO;
import com.bookverse.dto.response.voucher.UserVoucherResponseDTO;
import com.bookverse.dto.response.voucher.VoucherResponseDTO;
import com.bookverse.entity.Campaign;
import com.bookverse.entity.User;
import com.bookverse.entity.UserVoucher;
import com.bookverse.entity.Voucher;
import com.bookverse.enums.CampaignStatus;
import com.bookverse.enums.CampaignType;
import com.bookverse.enums.UserVoucherStatus;
import com.bookverse.enums.VoucherStatus;
import com.bookverse.mapper.VoucherMapper;
import com.bookverse.repository.CampaignRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.repository.UserVoucherRepository;
import com.bookverse.repository.VoucherRepository;
import com.bookverse.service.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final VoucherMapper voucherMapper;

    @Override
    @Transactional
    public UserVoucherResponseDTO claimVoucher(Long userId, Long voucherId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userVoucherRepository.existsByUserIdAndVoucherId(userId, voucherId)) {
            throw new BadRequestException("You have already claimed this voucher");
        }

        Voucher voucher = voucherRepository.findWithLockById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        Instant now = Instant.now();
        if (voucher.getStatus() != VoucherStatus.ACTIVE ||
            now.isBefore(voucher.getStartTime()) ||
            now.isAfter(voucher.getEndTime())) {
            throw new BadRequestException("Voucher is not active or out of claim period");
        }

        if (voucher.getClaimedQuantity() >= voucher.getTotalQuantity()) {
            throw new BadRequestException("Voucher is fully claimed");
        }

        voucher.setClaimedQuantity(voucher.getClaimedQuantity() + 1);
        voucherRepository.save(voucher);

        UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .status(UserVoucherStatus.UNUSED)
                .claimedAt(now)
                .expiresAt(voucher.getEndTime())
                .build();

        userVoucher = userVoucherRepository.save(userVoucher);
        return voucherMapper.toResponse(userVoucher);
    }

    @Override
    @Transactional
    public void grantWelcomeVoucher(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User {} not found when granting welcome voucher", userId);
            return;
        }

        Instant now = Instant.now();
        List<Campaign> welcomeCampaigns = campaignRepository.findActiveAutoDistributedCampaigns(
                CampaignType.WELCOME_GIFT, CampaignStatus.ACTIVE, now);

        for (Campaign campaign : welcomeCampaigns) {
            List<Voucher> availableVouchers = voucherRepository.findAvailableVouchersForCampaign(
                    campaign.getId(), VoucherStatus.ACTIVE, now);

            for (Voucher voucher : availableVouchers) {
                if (!userVoucherRepository.existsByUserIdAndVoucherId(userId, voucher.getId())) {
                    try {
                        claimVoucherInternal(user, voucher.getId(), now);
                    } catch (Exception e) {
                        log.error("Failed to grant welcome voucher {} to user {}", voucher.getId(), userId, e);
                    }
                }
            }
        }
    }

    private void claimVoucherInternal(User user, Long voucherId, Instant now) {
        Voucher voucher = voucherRepository.findWithLockById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        if (voucher.getClaimedQuantity() >= voucher.getTotalQuantity()) {
            return; // Fully claimed
        }

        voucher.setClaimedQuantity(voucher.getClaimedQuantity() + 1);
        voucherRepository.save(voucher);

        UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .status(UserVoucherStatus.UNUSED)
                .claimedAt(now)
                .expiresAt(now.plus(Duration.ofDays(7)))
                .build();

        userVoucherRepository.save(userVoucher);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<UserVoucherResponseDTO> getMyVouchers(Long userId, Pageable pageable) {
        Page<UserVoucher> page = userVoucherRepository.findByUserIdAndStatusAndExpiresAtGreaterThan(
            userId, UserVoucherStatus.UNUSED, Instant.now(), pageable
        );
        List<UserVoucherResponseDTO> dtos = page.getContent().stream()
            .map(voucherMapper::toResponse)
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
    public AdminVoucherResponseDTO createVoucherConfig(VoucherCreateRequestDTO request) {
        Campaign campaign = null;
        if (request.getCampaignId() != null) {
            campaign = campaignRepository.findById(request.getCampaignId())
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        }

        Voucher voucher = Voucher.builder()
            .campaign(campaign)
            .code(request.getCode())
            .name(request.getName())
            .discountType(request.getDiscountType())
            .discountValue(request.getDiscountValue())
            .maxDiscountAmount(request.getMaxDiscountAmount())
            .minOrderValue(request.getMinOrderValue())
            .totalQuantity(request.getTotalQuantity())
            .claimedQuantity(0)
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .status(request.getStatus())
            .build();
        voucher = voucherRepository.save(voucher);
        return voucherMapper.toAdminResponse(voucher);
    }

    @Override
    @Transactional
    public AdminVoucherResponseDTO updateVoucherConfig(Long id, VoucherUpdateRequestDTO request) {
        Voucher voucher = voucherRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        
        Campaign campaign = null;
        if (request.getCampaignId() != null) {
            campaign = campaignRepository.findById(request.getCampaignId())
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        }

        voucher.setCampaign(campaign);
        voucher.setCode(request.getCode());
        voucher.setName(request.getName());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setTotalQuantity(request.getTotalQuantity());
        voucher.setStartTime(request.getStartTime());
        voucher.setEndTime(request.getEndTime());
        voucher.setStatus(request.getStatus());
        
        voucher = voucherRepository.save(voucher);
        return voucherMapper.toAdminResponse(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AdminVoucherResponseDTO> getAllVoucherConfigs(Long campaignId, VoucherStatus status, Pageable pageable) {
        Page<Voucher> page;
        if (campaignId != null && status != null) {
            page = voucherRepository.findByCampaignIdAndStatus(campaignId, status, pageable);
        } else if (campaignId != null) {
            page = voucherRepository.findByCampaignId(campaignId, pageable);
        } else if (status != null) {
            page = voucherRepository.findByStatus(status, pageable);
        } else {
            page = voucherRepository.findAll(pageable);
        }
        
        List<AdminVoucherResponseDTO> dtos = page.getContent().stream()
            .map(voucherMapper::toAdminResponse)
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
    public void deleteVoucherConfig(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        voucher.setStatus(VoucherStatus.INACTIVE);
        voucherRepository.save(voucher);
    }

    @Override
    public PageResponseDTO<VoucherResponseDTO> getActiveVouchers(Pageable pageable) {
        Page<Voucher> page = voucherRepository.findActiveVouchers(Instant.now(), pageable);
        List<VoucherResponseDTO> dtos = page.getContent().stream()
            .map(voucherMapper::toResponse)
            .toList();
        return new PageResponseDTO<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
