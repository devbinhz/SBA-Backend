package com.bookverse.dto.response.order;

import com.bookverse.enums.DeliveryType;
import com.bookverse.enums.OrderStatus;
import com.bookverse.enums.PaymentProvider;
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
public class OrderResponseDTO {

    private Long id;
    private Long userId;
    private OrderStatus status;
    private Long subtotal;
    private Long shippingFee;
    private DeliveryType deliveryType;
    private Long giftWrapFee;
    private Long discountAmount;
    private Long total;
    private Object addressSnapshot;
    private PaymentProvider paymentMethod;
    private String shippingProvider;
    private String trackingCode;
    private Instant expiresAt;
    private Instant paidAt;
    private Instant shippedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderItemResponseDTO> items;
}
