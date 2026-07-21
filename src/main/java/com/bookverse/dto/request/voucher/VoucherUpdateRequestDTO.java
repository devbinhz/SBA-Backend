package com.bookverse.dto.request.voucher;

import com.bookverse.enums.DiscountType;
import com.bookverse.enums.VoucherStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.time.Instant;

@Data
public class VoucherUpdateRequestDTO {

    private Long campaignId;

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    private Long discountValue;

    @Min(value = 0, message = "Max discount amount must be greater than or equal to 0")
    private Long maxDiscountAmount;

    @NotNull(message = "Minimum order value is required")
    @Min(value = 0, message = "Minimum order value must be greater than or equal to 0")
    private Long minOrderValue;
    
    @NotNull(message = "Total quantity is required")
    @Positive(message = "Total quantity must be positive")
    private Integer totalQuantity;
    
    @NotNull(message = "Start time is required")
    private Instant startTime;
    
    @NotNull(message = "End time is required")
    private Instant endTime;
    
    @NotNull(message = "Status is required")
    private VoucherStatus status;
}
