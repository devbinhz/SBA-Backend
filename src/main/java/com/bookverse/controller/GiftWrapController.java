package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.dto.request.giftwrap.GiftWrapRequestDTO;
import com.bookverse.dto.response.giftwrap.GiftWrapResponseDTO;
import com.bookverse.service.giftwrap.GiftWrapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gift-wraps")
@RequiredArgsConstructor
@Tag(name = "Gift Wrap", description = "Checkout gift wrap catalog (patterns and fees) management APIs")
public class GiftWrapController {

    private final GiftWrapService giftWrapService;

    @GetMapping
    @Operation(summary = "Get active gift wrap options (Public)")
    public ApiResponse<List<GiftWrapResponseDTO>> getGiftWraps() {
        return ApiResponse.success(giftWrapService.getPublicGiftWraps());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get all gift wrap options including inactive (Admin)")
    public ApiResponse<List<GiftWrapResponseDTO>> getGiftWrapsAdmin() {
        return ApiResponse.success(giftWrapService.getAllGiftWraps());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new gift wrap option (Admin)")
    public ApiResponse<GiftWrapResponseDTO> createGiftWrap(@Valid @RequestBody GiftWrapRequestDTO request) {
        return ApiResponse.success(giftWrapService.createGiftWrap(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update an existing gift wrap option (Admin)")
    public ApiResponse<GiftWrapResponseDTO> updateGiftWrap(
            @PathVariable Long id,
            @Valid @RequestBody GiftWrapRequestDTO request) {
        return ApiResponse.success(giftWrapService.updateGiftWrap(id, request));
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Activate/deactivate a gift wrap option (Admin)")
    public ApiResponse<Void> setGiftWrapActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        boolean active = payload.getOrDefault("active", true);
        giftWrapService.setGiftWrapActive(id, active);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete a gift wrap option; rejected if already used in an order (Admin)")
    public ApiResponse<Void> deleteGiftWrap(@PathVariable Long id) {
        giftWrapService.deleteGiftWrap(id);
        return ApiResponse.success(null);
    }
}
