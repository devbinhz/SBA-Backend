package com.bookverse.dto.response.voucher;

import com.bookverse.enums.DiscountType;
import com.bookverse.enums.VoucherStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponseDTO {
    private Long id;
    private String code;
    private String name;
    private DiscountType discountType;
    private Long discountValue;
    private Long tierMinAmount;
    private VoucherStatus status;
    private Instant expiresAt;
}
