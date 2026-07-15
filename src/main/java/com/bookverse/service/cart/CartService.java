package com.bookverse.service.cart;

import com.bookverse.dto.request.cart.CartItemRequestDTO;
import com.bookverse.dto.request.cart.CartMergeRequestDTO;
import com.bookverse.dto.response.cart.CartResponseDTO;
import com.bookverse.entity.Cart;

public interface CartService {
    CartResponseDTO getCartResponse(Long userId);
    CartResponseDTO addCartItem(Long userId, CartItemRequestDTO requestDTO);
    CartResponseDTO mergeCart(Long userId, CartMergeRequestDTO requestDTO);
    CartResponseDTO updateCartItem(Long userId, Long itemId, CartItemRequestDTO requestDTO);
    CartResponseDTO deleteCartItem(Long userId, Long itemId);
    CartResponseDTO clearCart(Long userId);
    
    Cart getCartByUserId(Long userId);
    void clearCartByUserId(Long userId);
}
