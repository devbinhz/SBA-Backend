package com.bookverse.dto.request.refund;

import com.bookverse.enums.ResolutionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApproveRefundRequestDTO {

    @NotNull(message = "Resolution type is required")
    private ResolutionType resolutionType;

    @Size(max = 500, message = "Note must be at most 500 characters")
    private String note;
}
