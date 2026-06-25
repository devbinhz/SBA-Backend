package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.dto.request.cart.CartItemRequestDTO;
import com.bookverse.dto.response.cart.CartResponseDTO;
import com.bookverse.service.cart.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Cart management API")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get current user cart")
    public ResponseEntity<ApiResponse<CartResponseDTO>> getCart(@AuthenticationPrincipal(expression = "user.id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCartResponse(userId)));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add an item to the cart")
    public ResponseEntity<ApiResponse<CartResponseDTO>> addCartItem(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @Valid @RequestBody CartItemRequestDTO requestDTO) {
        return ResponseEntity.ok(ApiResponse.success(cartService.addCartItem(userId, requestDTO)));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartResponseDTO>> updateCartItem(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody CartItemRequestDTO requestDTO) {
        return ResponseEntity.ok(ApiResponse.success(cartService.updateCartItem(userId, itemId, requestDTO)));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Remove an item from the cart")
    public ResponseEntity<ApiResponse<CartResponseDTO>> deleteCartItem(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.deleteCartItem(userId, itemId)));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Clear the entire cart")
    public ResponseEntity<ApiResponse<CartResponseDTO>> clearCart(@AuthenticationPrincipal(expression = "user.id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.clearCart(userId)));
    }
}
