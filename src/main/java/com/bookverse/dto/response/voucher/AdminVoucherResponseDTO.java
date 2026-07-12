package com.bookverse.dto.response.voucher;

import com.bookverse.enums.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AdminVoucherResponseDTO {
    private Long id;
    private String name;
    private String codePrefix;
    private DiscountType discountType;
    private Long discountValue;
    private Long tierMinAmount;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
