package com.bookverse.integration.otp;

import com.bookverse.common.exception.QuotaExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisOtpStore implements OtpStore {

    private final StringRedisTemplate redisTemplate;

    @Value("${bookverse.otp.expiration-ms:600000}")
    private long otpExpirationMs;

    @Override
    public void storeOtp(String purpose, String identifier, String otpHash) {
        String key = buildKey(purpose, identifier);
        redisTemplate.opsForValue().set(key, otpHash, Duration.ofMillis(otpExpirationMs));
    }

    @Override
    public boolean verifyOtp(String purpose, String identifier, String otpHash) {
        String key = buildKey(purpose, identifier);
        String storedHash = redisTemplate.opsForValue().get(key);
        return storedHash != null && storedHash.equals(otpHash);
    }

    @Override
    public void deleteOtp(String purpose, String identifier) {
        String key = buildKey(purpose, identifier);
        redisTemplate.delete(key);
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
}
