package com.bookverse.integration.otp;

import com.bookverse.common.exception.QuotaExceededException;
import com.bookverse.common.util.HashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisOtpStoreTest {

    private static final String PURPOSE = "reset-password";
    private static final String EMAIL = "user@example.com";
    private static final String OTP = "123456";
    private static final String OTP_KEY = "otp:" + PURPOSE + ":" + EMAIL;
    private static final String ATTEMPT_KEY = "otp-attempts:" + PURPOSE + ":" + EMAIL;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisOtpStore otpStore;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpStore, "otpExpirationMs", 600000L);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(OTP_KEY)).thenReturn(HashUtils.sha256(OTP));
    }

    @Test
    void correctOtpVerifiesAndClearsAttemptCounter() {
        assertThat(otpStore.verifyOtp(PURPOSE, EMAIL, OTP)).isTrue();
        verify(redisTemplate).delete(ATTEMPT_KEY);
    }

    @Test
    void wrongOtpBelowLimitReturnsFalseWithoutDeletingOtp() {
        when(valueOperations.increment(ATTEMPT_KEY)).thenReturn(1L);

        assertThat(otpStore.verifyOtp(PURPOSE, EMAIL, "000000")).isFalse();

        verify(valueOperations).increment(ATTEMPT_KEY);
        verify(redisTemplate, never()).delete(OTP_KEY);
    }

    @Test
    void reachingMaxAttemptsDeletesOtpAndThrows() {
        when(valueOperations.increment(ATTEMPT_KEY)).thenReturn(5L);

        assertThatThrownBy(() -> otpStore.verifyOtp(PURPOSE, EMAIL, "000000"))
                .isInstanceOf(QuotaExceededException.class);

        verify(redisTemplate).delete(OTP_KEY);
        verify(redisTemplate).delete(ATTEMPT_KEY);
    }

    @Test
    void verifyMissingOtpReturnsFalseWithoutCountingAttempt() {
        when(valueOperations.get(OTP_KEY)).thenReturn(null);

        assertThat(otpStore.verifyOtp(PURPOSE, EMAIL, OTP)).isFalse();

        verify(valueOperations, never()).increment(any());
    }

    @Test
    void storeOtpResetsAttemptCounter() {
        otpStore.storeOtp(PURPOSE, EMAIL, OTP);

        verify(valueOperations).set(eq(OTP_KEY), any(), any());
        verify(redisTemplate).delete(ATTEMPT_KEY);
    }

    @Test
    void firstWrongAttemptSetsExpiryOnCounter() {
        when(valueOperations.increment(ATTEMPT_KEY)).thenReturn(1L);

        otpStore.verifyOtp(PURPOSE, EMAIL, "000000");

        verify(redisTemplate).expire(eq(ATTEMPT_KEY), any());
        verify(redisTemplate, times(0)).delete(OTP_KEY);
    }
}
