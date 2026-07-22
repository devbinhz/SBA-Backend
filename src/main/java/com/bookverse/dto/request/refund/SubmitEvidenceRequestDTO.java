package com.bookverse.dto.request.refund;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitEvidenceRequestDTO {

    @NotBlank(message = "Evidence URL is required")
    @Size(max = 500, message = "Evidence URL must be at most 500 characters")
    private String url;
}
