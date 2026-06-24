package com.bookverse.integration.mail;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResendMailService implements MailService {

    private final Resend resend;
    private final String fromEmail;

    public ResendMailService(
            @Value("${bookverse.resend.api-key}") String apiKey,
            @Value("${bookverse.resend.from-email}") String fromEmail) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendVerificationEmail(String to, String otp) {
        String subject = "Verify your email - BookVerse";
        String htmlBody = "<h1>Welcome to BookVerse!</h1>" +
                "<p>Your verification code is: <strong>" + otp + "</strong></p>" +
                "<p>This code will expire in 10 minutes.</p>";

        sendEmail(to, subject, htmlBody);
    }

    @Override
    public void sendPasswordResetEmail(String to, String otp) {
        String subject = "Password Reset Request - BookVerse";
        String htmlBody = "<h1>Reset Your Password</h1>" +
                "<p>Your password reset code is: <strong>" + otp + "</strong></p>" +
                "<p>This code will expire in 10 minutes.</p>";

        sendEmail(to, subject, htmlBody);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(to)
                .subject(subject)
                .html(htmlBody)
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            log.error("Failed to send email to {}", to, e);
        }
    }
}
