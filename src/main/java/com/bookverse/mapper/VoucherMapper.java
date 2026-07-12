package com.bookverse.mapper;

import com.bookverse.dto.response.voucher.VoucherResponseDTO;
import com.bookverse.entity.UserVoucher;
import org.springframework.stereotype.Component;

@Component
public class VoucherMapper {

    public VoucherResponseDTO toResponse(UserVoucher userVoucher) {
        if (userVoucher == null) {
            return null;
        }

        return VoucherResponseDTO.builder()
            .id(userVoucher.getId())
            .code(userVoucher.getCode())
            .name(userVoucher.getVoucher().getName())
            .discountType(userVoucher.getVoucher().getDiscountType())
            .discountValue(userVoucher.getVoucher().getDiscountValue())
            .tierMinAmount(userVoucher.getVoucher().getTierMinAmount())
            .status(userVoucher.getStatus())
            .expiresAt(userVoucher.getExpiresAt())
            .build();
    }

    public com.bookverse.dto.response.voucher.AdminVoucherResponseDTO toAdminResponse(com.bookverse.entity.Voucher voucher) {
        if (voucher == null) {
            return null;
        }

        return com.bookverse.dto.response.voucher.AdminVoucherResponseDTO.builder()
            .id(voucher.getId())
            .name(voucher.getName())
            .codePrefix(voucher.getCodePrefix())
            .discountType(voucher.getDiscountType())
            .discountValue(voucher.getDiscountValue())
            .tierMinAmount(voucher.getTierMinAmount())
            .active(voucher.isActive())
            .createdAt(voucher.getCreatedAt())
            .updatedAt(voucher.getUpdatedAt())
            .build();
    }
}
