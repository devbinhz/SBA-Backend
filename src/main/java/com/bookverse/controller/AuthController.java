package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.dto.request.auth.ForgotPasswordRequestDTO;
import com.bookverse.dto.request.auth.LoginRequestDTO;
import com.bookverse.dto.request.auth.LogoutRequestDTO;
import com.bookverse.dto.request.auth.RefreshTokenRequestDTO;
import com.bookverse.dto.request.auth.RegisterRequestDTO;
import com.bookverse.dto.request.auth.ResendVerificationRequestDTO;
import com.bookverse.dto.request.auth.ResetPasswordRequestDTO;
import com.bookverse.dto.request.auth.VerifyEmailRequestDTO;
import com.bookverse.dto.response.auth.RegisterResponseDTO;
import com.bookverse.dto.response.auth.TokenPairResponseDTO;
import com.bookverse.service.auth.AuthService;
import com.bookverse.service.auth.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication, registration, and token management")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "Register a new customer account")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ApiResponse.success(authService.register(request), "Registration successful. Please verify email.");
    }

    @Operation(summary = "Verify email using OTP")
    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequestDTO request) {
        authService.verifyEmail(request);
        return ApiResponse.success(null, "Email verified successfully");
    }

    @Operation(summary = "Resend verification OTP")
    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@Valid @RequestBody ResendVerificationRequestDTO request) {
        authService.resendVerification(request);
        return ApiResponse.success(null, "If your email is registered, a new OTP has been sent.");
    }

    @Operation(summary = "Login to get access and refresh tokens")
    @PostMapping("/login")
    public ApiResponse<TokenPairResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "Refresh token pair")
    @PostMapping("/refresh")
    public ApiResponse<TokenPairResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        return ApiResponse.success(refreshTokenService.refreshToken(request.getRefreshToken()));
    }

    @Operation(summary = "Logout by revoking the current refresh token")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequestDTO request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
    }

    @Operation(summary = "Request password reset OTP")
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        authService.forgotPassword(request);
        return ApiResponse.success(null, "If your email is registered, an OTP has been sent to reset your password.");
    }

    @Operation(summary = "Reset password using OTP")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ApiResponse.success(null, "Password reset successfully. All existing sessions have been revoked.");
    }
}
