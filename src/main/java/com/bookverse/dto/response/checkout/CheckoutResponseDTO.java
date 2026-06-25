package com.bookverse.dto.response.checkout;

import com.bookverse.enums.OrderStatus;
import com.bookverse.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDTO {

    private Long orderId;
    private Long paymentId;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private Long subtotal;
    private Long shippingFee;
    private Long total;
    private Long providerOrderCode;
    private String checkoutUrl;
    private Instant expiresAt;
    private List<CheckoutItemResponseDTO> items;
}
