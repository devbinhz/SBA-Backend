package com.bookverse.integration.mail;

public interface MailService {

    void sendVerificationEmail(String to, String otp);

    void sendPasswordResetEmail(String to, String otp);

    void sendGuestOrderConfirmationEmail(String to, String orderCode, String guestToken);

    void sendGuestOrderPaymentSuccessEmail(String to, String orderCode, String guestToken);
}
