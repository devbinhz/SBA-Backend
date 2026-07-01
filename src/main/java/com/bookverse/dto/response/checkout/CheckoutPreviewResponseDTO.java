package com.bookverse.dto.response.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutPreviewResponseDTO {

    private Long subtotal;
    private Long shippingFee;
    private Long discountAmount;
    private Long total;
    private List<CheckoutItemResponseDTO> items;
}
