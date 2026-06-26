package com.bookverse.mapper;

import com.bookverse.common.exception.BadRequestException;
import com.bookverse.dto.response.order.OrderItemResponseDTO;
import com.bookverse.dto.response.order.OrderResponseDTO;
import com.bookverse.dto.response.order.OrderStatusHistoryResponseDTO;
import com.bookverse.dto.response.order.OrderSummaryResponseDTO;
import com.bookverse.entity.Order;
import com.bookverse.entity.OrderItem;
import com.bookverse.entity.OrderStatusHistory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final ObjectMapper objectMapper;

    public OrderSummaryResponseDTO toSummary(Order order) {
        return OrderSummaryResponseDTO.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .shippingProvider(order.getShippingProvider())
                .trackingCode(order.getTrackingCode())
                .expiresAt(order.getExpiresAt())
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public OrderResponseDTO toDetail(Order order, List<OrderItem> items) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .addressSnapshot(parseAddressSnapshot(order.getAddressSnapshot()))
                .paymentMethod(order.getPaymentMethod())
                .shippingProvider(order.getShippingProvider())
                .trackingCode(order.getTrackingCode())
                .expiresAt(order.getExpiresAt())
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(items.stream().map(this::toItem).toList())
                .build();
    }

    public OrderItemResponseDTO toItem(OrderItem item) {
        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .bookId(item.getBook().getId())
                .title(item.getTitleSnapshot())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .lineTotal(item.getLineTotal())
                .build();
    }

    public OrderStatusHistoryResponseDTO toHistory(OrderStatusHistory history) {
        return OrderStatusHistoryResponseDTO.builder()
                .id(history.getId())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .changedBy(history.getChangedBy() == null ? null : history.getChangedBy().getId())
                .note(history.getNote())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private Object parseAddressSnapshot(String addressSnapshot) {
        try {
            return objectMapper.readValue(addressSnapshot, Object.class);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Order address snapshot is invalid");
        }
    }
}
