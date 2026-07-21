package com.bookverse.dto.response.voucher;

import com.bookverse.enums.DiscountType;
import com.bookverse.enums.VoucherStatus;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class AdminVoucherResponseDTO {
    private Long id;
    private Long campaignId;
    private String code;
    private String name;
    private DiscountType discountType;
    private Long discountValue;
    private Long maxDiscountAmount;
    private Long minOrderValue;
    private Integer totalQuantity;
    private Integer claimedQuantity;
    private Instant startTime;
    private Instant endTime;
    private VoucherStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
