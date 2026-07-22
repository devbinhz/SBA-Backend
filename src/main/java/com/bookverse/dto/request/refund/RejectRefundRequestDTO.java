package com.bookverse.dto.request.refund;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectRefundRequestDTO {

    @NotBlank(message = "A note is required when rejecting a return request")
    @Size(max = 500, message = "Note must be at most 500 characters")
    private String note;
}
