package com.bookverse.dto.response.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDTO {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private Long bookPrice;
    private String bookCoverUrl;
    private Integer quantity;
    private Long lineTotal;
    
    // Check if the book is still active and enough stock
    private Boolean available;
}
