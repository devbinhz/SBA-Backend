package com.bookverse.service.auth;

import com.bookverse.dto.request.auth.ForgotPasswordRequestDTO;
import com.bookverse.dto.request.auth.LoginRequestDTO;
import com.bookverse.dto.request.auth.RegisterRequestDTO;
import com.bookverse.dto.request.auth.ResendVerificationRequestDTO;
import com.bookverse.dto.request.auth.ResetPasswordRequestDTO;
import com.bookverse.dto.request.auth.VerifyEmailRequestDTO;
import com.bookverse.dto.response.auth.RegisterResponseDTO;
import com.bookverse.dto.response.auth.TokenPairResponseDTO;

public interface AuthService {

    RegisterResponseDTO register(RegisterRequestDTO request);

    void verifyEmail(VerifyEmailRequestDTO request);

    void resendVerification(ResendVerificationRequestDTO request);

    TokenPairResponseDTO login(LoginRequestDTO request);

    void forgotPassword(ForgotPasswordRequestDTO request);

    void resetPassword(ResetPasswordRequestDTO request);
}
