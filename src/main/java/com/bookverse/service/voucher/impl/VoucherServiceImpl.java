package com.bookverse.service.voucher.impl;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.response.voucher.VoucherResponseDTO;
import com.bookverse.entity.User;
import com.bookverse.entity.UserVoucher;
import com.bookverse.entity.Voucher;
import com.bookverse.enums.VoucherStatus;
import com.bookverse.mapper.VoucherMapper;
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

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final UserRepository userRepository;
    private final VoucherMapper voucherMapper;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<VoucherResponseDTO> getMyVouchers(Long userId, Pageable pageable) {
        Page<UserVoucher> page = userVoucherRepository.findByUserIdAndStatusAndExpiresAtGreaterThan(
            userId, VoucherStatus.UNUSED, Instant.now(), pageable
        );
        List<VoucherResponseDTO> dtos = page.getContent().stream()
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
    public void awardVoucherToUser(Long userId, Long orderAmount) {
        List<Voucher> eligibleVouchers = voucherRepository.findByActiveTrueAndTierMinAmountLessThanEqualOrderByTierMinAmountDesc(orderAmount);
        
        if (eligibleVouchers.isEmpty()) {
            log.info("No eligible vouchers found for user {} with order amount {}", userId, orderAmount);
            return;
        }

        // Randomly select one voucher from the eligible list
        Voucher selectedVoucher = eligibleVouchers.get(random.nextInt(eligibleVouchers.size()));
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User {} not found when awarding voucher", userId);
            return;
        }

        String randomSuffix = String.format("%04d", random.nextInt(10000));
        String prefix = selectedVoucher.getCodePrefix() != null ? selectedVoucher.getCodePrefix() : "BV";
        String code = prefix + "-" + Instant.now().toEpochMilli() % 100000 + "-" + randomSuffix;

        UserVoucher userVoucher = UserVoucher.builder()
            .user(user)
            .voucher(selectedVoucher)
            .code(code)
            .status(VoucherStatus.UNUSED)
            .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
            .build();

        userVoucherRepository.save(userVoucher);

        log.info("Gửi thông báo cho user {} nhận voucher {} (ẩn logic tier)", userId, code);
    }

    @Override
    @Transactional
    public com.bookverse.dto.response.voucher.AdminVoucherResponseDTO createVoucherConfig(com.bookverse.dto.request.voucher.VoucherCreateRequestDTO request) {
        Voucher voucher = Voucher.builder()
            .name(request.getName())
            .codePrefix(request.getCodePrefix())
            .discountType(request.getDiscountType())
            .discountValue(request.getDiscountValue())
            .tierMinAmount(request.getTierMinAmount())
            .active(request.isActive())
            .build();
        voucher = voucherRepository.save(voucher);
        return voucherMapper.toAdminResponse(voucher);
    }

    @Override
    @Transactional
    public com.bookverse.dto.response.voucher.AdminVoucherResponseDTO updateVoucherConfig(Long id, com.bookverse.dto.request.voucher.VoucherUpdateRequestDTO request) {
        Voucher voucher = voucherRepository.findById(id)
            .orElseThrow(() -> new com.bookverse.common.exception.ResourceNotFoundException("Voucher not found"));
        
        voucher.setName(request.getName());
        voucher.setCodePrefix(request.getCodePrefix());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setTierMinAmount(request.getTierMinAmount());
        voucher.setActive(request.isActive());
        
        voucher = voucherRepository.save(voucher);
        return voucherMapper.toAdminResponse(voucher);
    }

    @Override
    @Transactional
    public void deleteVoucherConfig(Long id) {
        Voucher voucher = voucherRepository.findById(id)
            .orElseThrow(() -> new com.bookverse.common.exception.ResourceNotFoundException("Voucher not found"));
        voucher.setActive(false);
        voucherRepository.save(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<com.bookverse.dto.response.voucher.AdminVoucherResponseDTO> getAllVoucherConfigs(Boolean active, Pageable pageable) {
        Page<Voucher> page;
        if (active != null) {
            page = voucherRepository.findByActive(active, pageable);
        } else {
            page = voucherRepository.findAll(pageable);
        }
        
        List<com.bookverse.dto.response.voucher.AdminVoucherResponseDTO> dtos = page.getContent().stream()
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
}
