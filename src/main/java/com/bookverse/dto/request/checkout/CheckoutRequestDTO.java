package com.bookverse.dto.request.checkout;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequestDTO {

    @NotNull(message = "Address id is required")
    private Long addressId;

    @NotEmpty(message = "At least one cart item must be selected")
    private List<@NotNull(message = "Cart item id is required") Long> cartItemIds;
}
