package com.bookverse.integration.otp;

public interface OtpStore {

    /**
     * Store OTP hash and return true if successful.
     */
    void storeOtp(String purpose, String identifier, String otpHash);

    /**
     * Check if the OTP hash exists and matches.
     */
    boolean verifyOtp(String purpose, String identifier, String otpHash);

    /**
     * Delete OTP after successful verification.
     */
    void deleteOtp(String purpose, String identifier);

    /**
     * Increment and check rate limit. Throws QuotaExceededException if exceeded.
     */
    void checkAndIncrementRateLimit(String purpose, String identifier);
}
