package com.bookverse.dto.request.refund;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompleteInspectionRequestDTO {

    @NotNull(message = "Inspection result is required")
    private Boolean passed;

    @Size(max = 500, message = "Note must be at most 500 characters")
    private String note;
}
