package com.bookverse.dto.response.order;

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
public class OrderStatusHistoryResponseDTO {

    private Long id;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private Long changedBy;
    private String note;
    private Instant createdAt;
}
