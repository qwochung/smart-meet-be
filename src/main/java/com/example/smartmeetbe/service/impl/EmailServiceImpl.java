package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    @Override
    public void sendVerificationEmail(String to, String token) {
        String link = frontendUrl + "/auth/verify-email?token=" + token;
        String subject = "SmartMeet – Verify your email";
        String body = buildVerificationHtml(link);
        sendHtmlEmail(to, subject, body);
    }


    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String link = frontendUrl + "/auth/reset-password?token=" + token;
        String subject = "SmartMeet – Reset your password";
        String body = buildPasswordResetHtml(link);
        sendHtmlEmail(to, subject, body);
    }


    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }


    private String buildVerificationHtml(String link) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:32px;background:#f9fafb;border-radius:12px">
                  <h2 style="color:#1a1a2e">Welcome to SmartMeet!</h2>
                  <p>Please verify your email address to activate your account.</p>
                  <a href="%s"
                     style="display:inline-block;margin-top:16px;padding:12px 28px;background:#4f46e5;color:#fff;
                            text-decoration:none;border-radius:8px;font-weight:600">
                    Verify Email
                  </a>
                  <p style="margin-top:24px;color:#6b7280;font-size:13px">
                    This link expires in 24 hours. If you didn't create an account, please ignore this email.
                  </p>
                </div>
                """.formatted(link);
    }

    private String buildPasswordResetHtml(String link) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:32px;background:#f9fafb;border-radius:12px">
                  <h2 style="color:#1a1a2e">Reset Your Password</h2>
                  <p>We received a request to reset your SmartMeet password.</p>
                  <a href="%s"
                     style="display:inline-block;margin-top:16px;padding:12px 28px;background:#4f46e5;color:#fff;
                            text-decoration:none;border-radius:8px;font-weight:600">
                    Reset Password
                  </a>
                  <p style="margin-top:24px;color:#6b7280;font-size:13px">
                    This link expires in 30 minutes. If you didn't request a password reset, please ignore this email.
                  </p>
                </div>
                """.formatted(link);
    }
}
