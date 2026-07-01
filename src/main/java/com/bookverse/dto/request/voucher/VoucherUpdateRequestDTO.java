package com.bookverse.dto.request.voucher;

import com.bookverse.enums.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class VoucherUpdateRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String codePrefix;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be positive")
    private Long discountValue;

    @NotNull(message = "Tier minimum amount is required")
    @Min(value = 0, message = "Tier minimum amount must be greater than or equal to 0")
    private Long tierMinAmount;

    private boolean active;
}
