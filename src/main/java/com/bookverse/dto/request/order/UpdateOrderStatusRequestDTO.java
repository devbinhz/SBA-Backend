package com.bookverse.dto.request.order;

import com.bookverse.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequestDTO {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    @Size(max = 100, message = "Shipping provider must be at most 100 characters")
    private String shippingProvider;

    @Size(max = 100, message = "Tracking code must be at most 100 characters")
    private String trackingCode;

    @Size(max = 500, message = "Note must be at most 500 characters")
    private String note;
}
