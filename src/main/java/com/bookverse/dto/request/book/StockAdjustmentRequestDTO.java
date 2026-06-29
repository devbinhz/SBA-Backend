package com.bookverse.dto.request.book;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 255, message = "Note must be at most 255 characters")
    private String note;
}
