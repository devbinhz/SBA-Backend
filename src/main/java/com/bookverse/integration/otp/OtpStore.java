package com.bookverse.integration.otp;

public interface OtpStore {

    /**
     * Store a hashed representation of the raw OTP.
     */
    void storeOtp(String purpose, String identifier, String rawOtp);

    /**
     * Hash the submitted raw OTP and check if it matches the stored hash.
     */
    boolean verifyOtp(String purpose, String identifier, String rawOtp);

    /**
     * Delete OTP after successful verification.
     */
    void deleteOtp(String purpose, String identifier);

    /**
     * Increment and check rate limit. Throws QuotaExceededException if exceeded.
     */
    void checkAndIncrementRateLimit(String purpose, String identifier);
}
