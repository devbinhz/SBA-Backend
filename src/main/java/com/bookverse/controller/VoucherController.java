package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.response.voucher.UserVoucherResponseDTO;
import com.bookverse.dto.response.voucher.VoucherResponseDTO;
import com.bookverse.service.voucher.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Tag(name = "Voucher", description = "Customer Voucher APIs")
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    @Operation(summary = "List all active vouchers (Public)")
    public ApiResponse<PageResponseDTO<VoucherResponseDTO>> getActiveVouchers(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(voucherService.getActiveVouchers(pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @Operation(summary = "Get current user's vouchers")
    public ApiResponse<PageResponseDTO<UserVoucherResponseDTO>> getMyVouchers(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @PageableDefault(sort = "claimedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(voucherService.getMyVouchers(userId, pageable));
    }
    
    @PostMapping("/claim/{voucherId}")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @Operation(summary = "Claim a voucher")
    public ApiResponse<UserVoucherResponseDTO> claimVoucher(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @PathVariable Long voucherId) {
        return ApiResponse.success(voucherService.claimVoucher(userId, voucherId));
    }
}
