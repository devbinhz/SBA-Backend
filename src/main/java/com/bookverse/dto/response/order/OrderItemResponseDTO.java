package com.bookverse.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDTO {

    private Long id;
    private Long bookId;
    private String title;
    private Long unitPrice;
    private Integer quantity;
    private Long lineTotal;
}
