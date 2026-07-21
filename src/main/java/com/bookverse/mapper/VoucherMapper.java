package com.bookverse.mapper;

import com.bookverse.dto.response.voucher.AdminVoucherResponseDTO;
import com.bookverse.dto.response.voucher.UserVoucherResponseDTO;
import com.bookverse.dto.response.voucher.VoucherResponseDTO;
import com.bookverse.entity.UserVoucher;
import com.bookverse.entity.Voucher;
import org.springframework.stereotype.Component;

@Component
public class VoucherMapper {

    public UserVoucherResponseDTO toResponse(UserVoucher userVoucher) {
        if (userVoucher == null) {
            return null;
        }

        return UserVoucherResponseDTO.builder()
            .id(userVoucher.getId())
            .voucherId(userVoucher.getVoucher().getId())
            .voucherCode(userVoucher.getVoucher().getCode())
            .voucherName(userVoucher.getVoucher().getName())
            .discountType(userVoucher.getVoucher().getDiscountType())
            .discountValue(userVoucher.getVoucher().getDiscountValue())
            .maxDiscountAmount(userVoucher.getVoucher().getMaxDiscountAmount())
            .minOrderValue(userVoucher.getVoucher().getMinOrderValue())
            .status(userVoucher.getStatus())
            .claimedAt(userVoucher.getClaimedAt())
            .expiresAt(userVoucher.getExpiresAt())
            .usedAt(userVoucher.getUsedAt())
            .build();
    }

    public AdminVoucherResponseDTO toAdminResponse(Voucher voucher) {
        if (voucher == null) {
            return null;
        }

        return AdminVoucherResponseDTO.builder()
            .id(voucher.getId())
            .campaignId(voucher.getCampaign() != null ? voucher.getCampaign().getId() : null)
            .code(voucher.getCode())
            .name(voucher.getName())
            .discountType(voucher.getDiscountType())
            .discountValue(voucher.getDiscountValue())
            .maxDiscountAmount(voucher.getMaxDiscountAmount())
            .minOrderValue(voucher.getMinOrderValue())
            .totalQuantity(voucher.getTotalQuantity())
            .claimedQuantity(voucher.getClaimedQuantity())
            .startTime(voucher.getStartTime())
            .endTime(voucher.getEndTime())
            .status(voucher.getStatus())
            .createdAt(voucher.getCreatedAt())
            .updatedAt(voucher.getUpdatedAt())
            .build();
    }

    public VoucherResponseDTO toResponse(Voucher voucher) {
        if (voucher == null) {
            return null;
        }

        return VoucherResponseDTO.builder()
            .id(voucher.getId())
            .campaignId(voucher.getCampaign() != null ? voucher.getCampaign().getId() : null)
            .code(voucher.getCode())
            .name(voucher.getName())
            .discountType(voucher.getDiscountType())
            .discountValue(voucher.getDiscountValue())
            .maxDiscountAmount(voucher.getMaxDiscountAmount())
            .minOrderValue(voucher.getMinOrderValue())
            .totalQuantity(voucher.getTotalQuantity())
            .claimedQuantity(voucher.getClaimedQuantity())
            .startTime(voucher.getStartTime())
            .endTime(voucher.getEndTime())
            .build();
    }
}
