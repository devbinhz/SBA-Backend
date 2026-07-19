package com.bookverse.integration.otp;

import com.bookverse.common.exception.QuotaExceededException;
import com.bookverse.common.util.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisOtpStore implements OtpStore {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_VERIFY_ATTEMPTS = 5;

    @Value("${bookverse.otp.expiration-ms:600000}")
    private long otpExpirationMs;

    @Override
    public void storeOtp(String purpose, String identifier, String rawOtp) {
        String key = buildKey(purpose, identifier);
        redisTemplate.opsForValue().set(key, HashUtils.sha256(rawOtp), Duration.ofMillis(otpExpirationMs));
        // Reset any leftover attempt counter from a previous code.
        redisTemplate.delete(attemptKey(purpose, identifier));
    }

    @Override
    public boolean verifyOtp(String purpose, String identifier, String rawOtp) {
        String key = buildKey(purpose, identifier);
        String storedHash = redisTemplate.opsForValue().get(key);
        if (storedHash == null) {
            return false;
        }

        if (storedHash.equals(HashUtils.sha256(rawOtp))) {
            redisTemplate.delete(attemptKey(purpose, identifier));
            return true;
        }

        // Wrong code: count the failed attempt and lock the OTP out once the ceiling is hit.
        String attemptKey = attemptKey(purpose, identifier);
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptKey, Duration.ofMillis(otpExpirationMs));
        }
        if (attempts != null && attempts >= MAX_VERIFY_ATTEMPTS) {
            redisTemplate.delete(key);
            redisTemplate.delete(attemptKey);
            throw new QuotaExceededException("Too many invalid OTP attempts. Please request a new code.");
        }
        return false;
    }

    @Override
    public void deleteOtp(String purpose, String identifier) {
        String key = buildKey(purpose, identifier);
        redisTemplate.delete(key);
        redisTemplate.delete(attemptKey(purpose, identifier));
    }

    @Override
    public void checkAndIncrementRateLimit(String purpose, String identifier) {
        String key = "rate-limit:" + buildKey(purpose, identifier);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return;
        }
        if (count == 1) {
            // Expire after 1 minute for rate limiting window
            redisTemplate.expire(key, Duration.ofMinutes(1));
        } else if (count > 3) {
            throw new QuotaExceededException("Rate limit exceeded for OTP generation. Please try again later.");
        }
    }

    private String buildKey(String purpose, String identifier) {
        return "otp:" + purpose + ":" + identifier;
    }

    private String attemptKey(String purpose, String identifier) {
        return "otp-attempts:" + purpose + ":" + identifier;
    }
}
