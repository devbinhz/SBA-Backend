package com.bookverse.service.voucher;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.response.voucher.VoucherResponseDTO;
import org.springframework.data.domain.Pageable;

public interface VoucherService {
    PageResponseDTO<VoucherResponseDTO> getMyVouchers(Long userId, Pageable pageable);
    void awardVoucherToUser(Long userId, Long orderAmount);
}
