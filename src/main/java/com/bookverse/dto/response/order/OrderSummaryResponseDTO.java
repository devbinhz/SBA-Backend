package com.bookverse.dto.response.order;

import com.bookverse.enums.DeliveryType;
import com.bookverse.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponseDTO {

    private Long id;
    private Long userId;
    private String guestEmail;
    private OrderStatus status;
    private Long subtotal;
    private Long shippingFee;
    private DeliveryType deliveryType;
    private Long giftWrapFee;
    private Long giftWrapId;
    private String giftWrapName;
    private Long discountAmount;
    private Long total;
    private String shippingProvider;
    private String trackingCode;
    private Instant expiresAt;
    private Instant paidAt;
    private Instant shippedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    private Instant createdAt;
}
