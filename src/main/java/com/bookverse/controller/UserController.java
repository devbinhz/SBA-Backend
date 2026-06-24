package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.common.dto.PageResponseDTO;
import com.bookverse.dto.request.address.AddressRequestDTO;
import com.bookverse.dto.request.user.ChangePasswordRequestDTO;
import com.bookverse.dto.request.user.SetUserEnabledRequestDTO;
import com.bookverse.dto.request.user.UpdateProfileRequestDTO;
import com.bookverse.dto.response.address.AddressResponseDTO;
import com.bookverse.dto.response.user.UserResponseDTO;
import com.bookverse.service.address.AddressService;
import com.bookverse.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile, address, and admin user management endpoints")
public class UserController {

    private final UserService userService;
    private final AddressService addressService;

    @Operation(summary = "Get current user profile")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile returned")
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<UserResponseDTO> getProfile(@AuthenticationPrincipal(expression = "user.id") Long userId) {
        return ApiResponse.success(userService.getProfile(userId));
    }

    @Operation(summary = "Update current user profile")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated")
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<UserResponseDTO> updateProfile(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @Valid @RequestBody UpdateProfileRequestDTO request) {
        return ApiResponse.success(userService.updateProfile(userId, request));
    }

    @Operation(summary = "Change current user password")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed")
    @PutMapping("/me/password")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @Valid @RequestBody ChangePasswordRequestDTO request) {
        userService.changePassword(userId, request);
        return ApiResponse.success(null, "Password changed successfully. All existing sessions have been revoked.");
    }

    @Operation(summary = "List current user's addresses")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Addresses returned")
    @GetMapping("/me/addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<AddressResponseDTO>> listAddresses(@AuthenticationPrincipal(expression = "user.id") Long userId) {
        return ApiResponse.success(addressService.listAddresses(userId));
    }

    @Operation(summary = "Create an address for current user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Address created")
    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<AddressResponseDTO> createAddress(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @Valid @RequestBody AddressRequestDTO request) {
        return ApiResponse.success(addressService.createAddress(userId, request), "Address created successfully");
    }

    @Operation(summary = "Update current user's address")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address updated")
    @PutMapping("/me/addresses/{addressId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<AddressResponseDTO> updateAddress(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequestDTO request) {
        return ApiResponse.success(addressService.updateAddress(userId, addressId, request));
    }

    @Operation(summary = "Delete current user's address")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Address deleted")
    @DeleteMapping("/me/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CUSTOMER')")
    public void deleteAddress(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
    }

    @Operation(summary = "Set current user's default address")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Default address changed")
    @PutMapping("/me/addresses/{addressId}/default")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<AddressResponseDTO> setDefaultAddress(
            @AuthenticationPrincipal(expression = "user.id") Long userId,
            @PathVariable Long addressId) {
        return ApiResponse.success(addressService.setDefaultAddress(userId, addressId));
    }

    @Operation(summary = "List users for admin")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users returned")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponseDTO<UserResponseDTO>> listUsers(@ParameterObject Pageable pageable) {
        return ApiResponse.success(userService.listUsers(pageable));
    }

    @Operation(summary = "Enable or disable a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User enabled state updated")
    @PutMapping("/{userId}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponseDTO> setEnabled(
            @AuthenticationPrincipal(expression = "user.id") Long adminUserId,
            @PathVariable Long userId,
            @Valid @RequestBody SetUserEnabledRequestDTO request) {
        return ApiResponse.success(userService.setEnabled(adminUserId, userId, request.getEnabled()));
    }
}
