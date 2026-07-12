package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.voucher.VoucherCreateRequestDTO;
import com.bookverse.dto.request.voucher.VoucherUpdateRequestDTO;
import com.bookverse.dto.response.voucher.AdminVoucherResponseDTO;
import com.bookverse.dto.response.voucher.VoucherResponseDTO;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Tag(name = "Voucher", description = "Voucher Management APIs")
public class VoucherController {

    private final VoucherService voucherService;

    // --- CUSTOMER APIS ---

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @Operation(summary = "Get current user's vouchers (Customer)")
    public ApiResponse<PageResponseDTO<VoucherResponseDTO>> getMyVouchers(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            Pageable pageable) {
        return ApiResponse.success(voucherService.getMyVouchers(userId, pageable));
    }

    // --- ADMIN APIS ---

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create a new voucher config (Admin)")
    public ApiResponse<AdminVoucherResponseDTO> createVoucher(@Valid @RequestBody VoucherCreateRequestDTO request) {
        AdminVoucherResponseDTO response = voucherService.createVoucherConfig(request);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update a voucher config (Admin)")
    public ApiResponse<AdminVoucherResponseDTO> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody VoucherUpdateRequestDTO request) {
        AdminVoucherResponseDTO response = voucherService.updateVoucherConfig(id, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete (soft) a voucher config (Admin)")
    public void deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucherConfig(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get all voucher configs (Admin)")
    public ApiResponse<PageResponseDTO<AdminVoucherResponseDTO>> getVouchers(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponseDTO<AdminVoucherResponseDTO> response = voucherService.getAllVoucherConfigs(active, pageable);
        return ApiResponse.success(response);
    }
}
