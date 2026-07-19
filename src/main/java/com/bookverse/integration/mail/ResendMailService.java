package com.bookverse.integration.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service("resendMailService")
@Slf4j
public class ResendMailService implements MailService {

    private final RestClient restClient;
    private final String fromEmail;
    private final String apiKey;

    public ResendMailService(
            @Value("${bookverse.resend.api-key}") String apiKey,
            @Value("${bookverse.resend.from-email}") String fromEmail) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
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
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Resend API key is not configured, skipping email delivery to {}", to);
            throw new IllegalStateException("Resend API key is missing");
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "from", fromEmail,
                    "to", List.of(to),
                    "subject", subject,
                    "html", htmlBody
            );

            log.info("Sending email via Resend API to {}", to);
            restClient.post()
                    .uri("/emails")
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully sent email via Resend API to {}", to);
        } catch (Exception exception) {
            log.error("Failed to send email via Resend API to {}", to, exception);
            throw new IllegalStateException("Failed to send email via Resend", exception);
        }
    }
}
