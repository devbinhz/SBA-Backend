package com.bookverse.dto.request.refund;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApproveRefundRequestDTO {

    @Size(max = 500, message = "Note must be at most 500 characters")
    private String note;
}
