package com.bookverse.dto.response.voucher;

import com.bookverse.enums.DiscountType;
import com.bookverse.enums.UserVoucherStatus;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class UserVoucherResponseDTO {
    private Long id;
    private Long voucherId;
    private String voucherCode;
    private String voucherName;
    private DiscountType discountType;
    private Long discountValue;
    private Long maxDiscountAmount;
    private Long minOrderValue;
    private UserVoucherStatus status;
    private Instant claimedAt;
    private Instant expiresAt;
    private Instant usedAt;
}
