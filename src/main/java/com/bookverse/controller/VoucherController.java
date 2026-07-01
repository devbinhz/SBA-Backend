package com.bookverse.controller;

import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.response.voucher.VoucherResponseDTO;
import com.bookverse.security.SecurityUser;
import com.bookverse.service.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PageResponseDTO<VoucherResponseDTO> getMyVouchers(
            @AuthenticationPrincipal SecurityUser userDetails,
            Pageable pageable) {
        return voucherService.getMyVouchers(userDetails.getUser().getId(), pageable);
    }
}
