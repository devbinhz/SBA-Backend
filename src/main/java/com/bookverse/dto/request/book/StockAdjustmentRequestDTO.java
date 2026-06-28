package com.bookverse.dto.request.book;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequestDTO {

    @NotNull(message = "Quantity delta is required")
    private Integer quantityDelta;

    private String note;
}
