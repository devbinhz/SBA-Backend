package com.bookverse.service.auth;

import com.bookverse.entity.RefreshToken;
import com.bookverse.entity.User;
import com.bookverse.dto.response.auth.TokenPairResponseDTO;

public interface RefreshTokenService {

    /**
     * Generate a new token pair and save refresh token.
     */
    TokenPairResponseDTO createTokenFamily(User user, String accessToken, long accessExpiresIn);

    /**
     * Refresh the token pair. Rotate refresh token or revoke family if reuse detected.
     */
    TokenPairResponseDTO refreshToken(String rawRefreshToken);

    /**
     * Revoke a single refresh token (used in Logout).
     */
    void revokeToken(String rawRefreshToken);

    /**
     * Revoke all refresh tokens for a user (used in Reset Password).
     */
    void revokeAllUserTokens(Long userId);
}
