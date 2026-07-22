package com.bookverse.dto.request.refund;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitReturnShipmentRequestDTO {

    @NotBlank(message = "Shipping provider is required")
    @Size(max = 100, message = "Shipping provider must be at most 100 characters")
    private String shippingProvider;

    @NotBlank(message = "Tracking code is required")
    @Size(max = 100, message = "Tracking code must be at most 100 characters")
    private String trackingCode;
}
