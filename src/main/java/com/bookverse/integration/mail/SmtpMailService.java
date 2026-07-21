package com.bookverse.integration.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service("mailService")
@RequiredArgsConstructor
@Slf4j
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;

    @Value("${bookverse.mail.from-email}")
    private String fromEmail;

    @Value("${bookverse.order.guest-track-url}")
    private String guestTrackUrl;

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

    @Override
    public void sendGuestOrderConfirmationEmail(String to, String orderCode, String guestToken) {
        String subject = "Order Confirmation - BookVerse";
        String magicLink = guestTrackUrl + "?code=" + orderCode + "&token=" + guestToken;
        String htmlBody = "<h1>Thank you for your order!</h1>" +
                "<p>Your order code is: <strong>" + orderCode + "</strong></p>" +
                "<p>You can track your order status using the following link:</p>" +
                "<p><a href=\"" + magicLink + "\">Track My Order</a></p>";

        sendEmail(to, subject, htmlBody);
    }

    @Override
    public void sendGuestOrderPaymentSuccessEmail(String to, String orderCode, String guestToken) {
        String subject = "Payment Successful - BookVerse";
        String magicLink = guestTrackUrl + "?code=" + orderCode + "&token=" + guestToken;
        String htmlBody = "<h1>Payment Successful!</h1>" +
                "<p>We have successfully received the payment for your order: <strong>" + orderCode + "</strong></p>" +
                "<p>You can track your order status using the following link:</p>" +
                "<p><a href=\"" + magicLink + "\">Track My Order</a></p>";

        sendEmail(to, subject, htmlBody);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            log.error("Failed to send email to {}", to, exception);
            throw new IllegalStateException("Failed to send email", exception);
        }
    }
}
