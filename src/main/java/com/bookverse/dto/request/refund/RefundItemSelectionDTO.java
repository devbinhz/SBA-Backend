package com.bookverse.dto.request.refund;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundItemSelectionDTO {

    @NotNull(message = "Order item id is required")
    private Long orderItemId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
