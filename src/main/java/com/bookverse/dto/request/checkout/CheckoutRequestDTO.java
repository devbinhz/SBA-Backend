package com.bookverse.dto.request.checkout;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequestDTO {

    @NotNull(message = "Address id is required")
    private Long addressId;
}
