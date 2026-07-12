package com.bookverse.service.voucher;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.response.voucher.VoucherResponseDTO;
import org.springframework.data.domain.Pageable;

public interface VoucherService {
    PageResponseDTO<VoucherResponseDTO> getMyVouchers(Long userId, Pageable pageable);
    void awardVoucherToUser(Long userId, Long orderAmount);
    
    com.bookverse.dto.response.voucher.AdminVoucherResponseDTO createVoucherConfig(com.bookverse.dto.request.voucher.VoucherCreateRequestDTO request);
    com.bookverse.dto.response.voucher.AdminVoucherResponseDTO updateVoucherConfig(Long id, com.bookverse.dto.request.voucher.VoucherUpdateRequestDTO request);
    void deleteVoucherConfig(Long id);
    PageResponseDTO<com.bookverse.dto.response.voucher.AdminVoucherResponseDTO> getAllVoucherConfigs(Boolean active, Pageable pageable);
}
