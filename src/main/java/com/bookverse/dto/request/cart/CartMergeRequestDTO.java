package com.bookverse.dto.request.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartMergeRequestDTO {

    @NotEmpty(message = "Cart items cannot be empty")
    @Valid
    private List<CartItemRequestDTO> items;
}
