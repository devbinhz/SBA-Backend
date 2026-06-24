package com.bookverse.service.auth.impl;

import com.bookverse.common.exception.AccountDisabledException;
import com.bookverse.common.exception.ConflictException;
import com.bookverse.common.exception.EmailNotVerifiedException;
import com.bookverse.common.exception.OtpExpiredException;
import com.bookverse.common.exception.OtpInvalidException;
import com.bookverse.common.exception.ResourceNotFoundException;
import com.bookverse.common.exception.UnauthorizedException;
import com.bookverse.dto.request.auth.ForgotPasswordRequestDTO;
import com.bookverse.dto.request.auth.LoginRequestDTO;
import com.bookverse.dto.request.auth.RegisterRequestDTO;
import com.bookverse.dto.request.auth.ResendVerificationRequestDTO;
import com.bookverse.dto.request.auth.ResetPasswordRequestDTO;
import com.bookverse.dto.request.auth.VerifyEmailRequestDTO;
import com.bookverse.dto.response.auth.RegisterResponseDTO;
import com.bookverse.dto.response.auth.TokenPairResponseDTO;
import com.bookverse.entity.User;
import com.bookverse.integration.mail.MailService;
import com.bookverse.integration.otp.OtpStore;
import com.bookverse.repository.UserRepository;
import com.bookverse.security.JwtService;
import com.bookverse.service.auth.AuthService;
import com.bookverse.service.auth.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpStore otpStore;
    private final MailService mailService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${bookverse.security.jwt.access-expiration-ms:900000}")
    private long accessExpirationMs;

    @Override
    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();
        
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already in use");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .build();

        userRepository.save(user);

        String otp = generateNumericOtp(6);
        otpStore.storeOtp("verify-email", email, otp);
        mailService.sendVerificationEmail(email, otp);

        return RegisterResponseDTO.builder()
                .email(email)
                .message("Registration successful. Please check your email for the verification code.")
                .build();
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();

        if (!otpStore.verifyOtp("verify-email", email, request.getOtp())) {
            throw new OtpInvalidException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            throw new ConflictException("Email is already verified");
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);

        otpStore.deleteOtp("verify-email", email);
    }

    @Override
    public void resendVerification(ResendVerificationRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();
        
        otpStore.checkAndIncrementRateLimit("verify-email-resend", email);

        // We don't throw exception if user doesn't exist to prevent email enumeration
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                String otp = generateNumericOtp(6);
                otpStore.storeOtp("verify-email", email, otp);
                mailService.sendVerificationEmail(email, otp);
            }
        });
    }

    @Override
    @Transactional
    public TokenPairResponseDTO login(LoginRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new AccountDisabledException("Account is disabled");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email is not verified");
        }

        String accessToken = jwtService.generateAccessToken(user);

        return refreshTokenService.createTokenFamily(user, accessToken, accessExpirationMs);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();
        
        otpStore.checkAndIncrementRateLimit("forgot-password", email);

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (user.isEnabled()) {
                String otp = generateNumericOtp(6);
                otpStore.storeOtp("reset-password", email, otp);
                mailService.sendPasswordResetEmail(email, otp);
            }
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();

        if (!otpStore.verifyOtp("reset-password", email, request.getOtp())) {
            throw new OtpInvalidException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpStore.deleteOtp("reset-password", email);

        // Revoke all refresh tokens
        refreshTokenService.revokeAllUserTokens(user.getId());
    }

    private String generateNumericOtp(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
