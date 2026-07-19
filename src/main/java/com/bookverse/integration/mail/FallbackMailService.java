package com.bookverse.integration.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service("mailService")
@Primary
@Slf4j
public class FallbackMailService implements MailService {

    private final MailService smtpMailService;
    private final MailService resendMailService;

    public FallbackMailService(
            @Qualifier("smtpMailService") MailService smtpMailService,
            @Qualifier("resendMailService") MailService resendMailService) {
        this.smtpMailService = smtpMailService;
        this.resendMailService = resendMailService;
    }

    @Override
    public void sendVerificationEmail(String to, String otp) {
        try {
            log.info("Attempting to send verification email via SMTP to {}", to);
            smtpMailService.sendVerificationEmail(to, otp);
            log.info("Successfully sent verification email via SMTP to {}", to);
        } catch (Exception exception) {
            log.warn("SMTP email delivery failed for {}, falling back to Resend API. Error: {}", to, exception.getMessage());
            resendMailService.sendVerificationEmail(to, otp);
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String otp) {
        try {
            log.info("Attempting to send password reset email via SMTP to {}", to);
            smtpMailService.sendPasswordResetEmail(to, otp);
            log.info("Successfully sent password reset email via SMTP to {}", to);
        } catch (Exception exception) {
            log.warn("SMTP email delivery failed for {}, falling back to Resend API. Error: {}", to, exception.getMessage());
            resendMailService.sendPasswordResetEmail(to, otp);
        }
    }
}
