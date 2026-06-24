package com.bookverse.service.auth.impl;

import com.bookverse.common.exception.RefreshTokenException;
import com.bookverse.entity.RefreshToken;
import com.bookverse.entity.User;
import com.bookverse.repository.RefreshTokenRepository;
import com.bookverse.repository.UserRepository;
import com.bookverse.security.JwtService;
import com.bookverse.service.auth.RefreshTokenService;
import com.bookverse.dto.response.auth.TokenPairResponseDTO;
import com.bookverse.dto.response.user.UserResponseDTO;
import com.bookverse.common.util.HashUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;

    @Value("${bookverse.security.refresh-token.expiration-ms}")
    private long refreshExpirationMs;

    @Value("${bookverse.security.refresh-token.bytes}")
    private int refreshTokenBytes;

    @Value("${bookverse.security.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Override
    @Transactional
    public TokenPairResponseDTO createTokenFamily(User user, String accessToken, long accessExpiresIn) {
        String rawToken = generateRandomToken();
        String hash = HashUtils.sha256(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .familyId(UUID.randomUUID())
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        refreshTokenRepository.save(refreshToken);

        return TokenPairResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(rawToken)
                .tokenType("Bearer")
                .accessExpiresIn(accessExpiresIn / 1000)
                .refreshExpiresIn(refreshExpirationMs / 1000)
                .user(modelMapper.map(user, UserResponseDTO.class))
                .build();
    }

    @Override
    @Transactional
    public TokenPairResponseDTO refreshToken(String rawRefreshToken) {
        String hash = HashUtils.sha256(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new RefreshTokenException("REFRESH_TOKEN_INVALID", "Invalid refresh token"));

        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenException("REFRESH_TOKEN_EXPIRED", "Refresh token is expired or revoked");
        }

        if (token.isUsed()) {
            // Reuse detected -> Revoke family
            refreshTokenRepository.revokeAllByFamily(token.getFamilyId(), Instant.now());
            throw new RefreshTokenException("REFRESH_TOKEN_REUSE_DETECTED", "Refresh token reuse detected. Family revoked.");
        }

        User user = token.getUser();
        if (!user.isEnabled()) {
            throw new RefreshTokenException("ACCOUNT_DISABLED", "Account is disabled");
        }

        // Mark old token as used
        token.setUsed(true);
        token.setUsedAt(Instant.now());

        // Create new token
        String newRawToken = generateRandomToken();
        String newHash = HashUtils.sha256(newRawToken);

        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newHash)
                .familyId(token.getFamilyId()) // Same family
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        refreshTokenRepository.save(newToken);
        
        token.setReplacedByTokenId(newToken.getId());
        refreshTokenRepository.save(token);

        String newAccessToken = jwtService.generateAccessToken(user);

        return TokenPairResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawToken)
                .tokenType("Bearer")
                .accessExpiresIn(accessExpirationMs / 1000)
                .refreshExpiresIn(refreshExpirationMs / 1000)
                .build();
    }

    @Override
    @Transactional
    public void revokeToken(String rawRefreshToken) {
        String hash = HashUtils.sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUser(userId, Instant.now());
    }

    private String generateRandomToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[refreshTokenBytes];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
