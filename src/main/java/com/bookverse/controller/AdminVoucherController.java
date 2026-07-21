package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.voucher.VoucherCreateRequestDTO;
import com.bookverse.dto.request.voucher.VoucherUpdateRequestDTO;
import com.bookverse.dto.response.voucher.AdminVoucherResponseDTO;
import com.bookverse.enums.VoucherStatus;
import com.bookverse.service.voucher.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
@Tag(name = "Admin Voucher", description = "Admin Voucher Config APIs")
public class AdminVoucherController {

    private final VoucherService voucherService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create a voucher template")
    public ApiResponse<AdminVoucherResponseDTO> createVoucher(@Valid @RequestBody VoucherCreateRequestDTO request) {
        return ApiResponse.success(voucherService.createVoucherConfig(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update a voucher template")
    public ApiResponse<AdminVoucherResponseDTO> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody VoucherUpdateRequestDTO request) {
        return ApiResponse.success(voucherService.updateVoucherConfig(id, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "List all voucher templates")
    public ApiResponse<PageResponseDTO<AdminVoucherResponseDTO>> getVouchers(
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) VoucherStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(voucherService.getAllVoucherConfigs(campaignId, status, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Deactivate a voucher template (Soft Delete)")
    public ApiResponse<Void> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucherConfig(id);
        return ApiResponse.success(null);
    }
}
