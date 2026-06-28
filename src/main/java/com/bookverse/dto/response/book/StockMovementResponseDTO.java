package com.bookverse.dto.response.book;

import com.bookverse.enums.StockMovementReason;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StockMovementResponseDTO {
    private Long id;
    private Long bookId;
    private Long orderId;
    private Integer delta;
    private StockMovementReason reason;
    private String operationKey;
    private String note;
    private Long createdBy;
    private String createdByName; // We can enrich this in the service
    private Instant createdAt;
}
