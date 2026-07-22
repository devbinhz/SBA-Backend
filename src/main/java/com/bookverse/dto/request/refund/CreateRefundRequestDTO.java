package com.bookverse.dto.request.refund;

import com.bookverse.enums.RefundReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateRefundRequestDTO {

    @NotEmpty(message = "At least one order item must be selected for return")
    @Valid
    private List<RefundItemSelectionDTO> items;

    @NotNull(message = "Refund reason is required")
    private RefundReason reason;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @NotBlank(message = "Bank name is required")
    @Size(max = 255, message = "Bank name must be at most 255 characters")
    private String bankName;

    @NotBlank(message = "Bank account number is required")
    @Size(max = 50, message = "Bank account number must be at most 50 characters")
    private String bankAccountNumber;

    @NotBlank(message = "Bank account holder is required")
    @Size(max = 255, message = "Bank account holder must be at most 255 characters")
    private String bankAccountHolder;

    @NotEmpty(message = "At least one evidence URL is required")
    private List<@NotBlank @Size(max = 500) String> evidenceUrls;
}
