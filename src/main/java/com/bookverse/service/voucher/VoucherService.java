package com.bookverse.service.voucher;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.voucher.VoucherCreateRequestDTO;
import com.bookverse.dto.request.voucher.VoucherUpdateRequestDTO;
import com.bookverse.dto.response.voucher.AdminVoucherResponseDTO;
import com.bookverse.dto.response.voucher.UserVoucherResponseDTO;
import com.bookverse.dto.response.voucher.VoucherResponseDTO;
import com.bookverse.enums.VoucherStatus;
import org.springframework.data.domain.Pageable;

public interface VoucherService {
    UserVoucherResponseDTO claimVoucher(Long userId, Long voucherId);
    void grantWelcomeVoucher(Long userId);
    PageResponseDTO<UserVoucherResponseDTO> getMyVouchers(Long userId, Pageable pageable);
    
    AdminVoucherResponseDTO createVoucherConfig(VoucherCreateRequestDTO request);
    AdminVoucherResponseDTO updateVoucherConfig(Long id, VoucherUpdateRequestDTO request);
    PageResponseDTO<AdminVoucherResponseDTO> getAllVoucherConfigs(Long campaignId, VoucherStatus status, Pageable pageable);
    void deleteVoucherConfig(Long id);
    
    PageResponseDTO<VoucherResponseDTO> getActiveVouchers(Pageable pageable);
}
