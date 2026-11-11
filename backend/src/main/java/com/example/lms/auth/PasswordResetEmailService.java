package com.example.lms.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetEmailService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenders;
    private final boolean enabled;
    private final String smtpHost;
    private final String senderAddress;
    private final String resetUrl;

    public PasswordResetEmailService(
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${app.mail.from:}") String senderAddress,
            @Value("${app.auth.password-reset-url:}") String resetUrl) {
        this.mailSenders = mailSenders;
        this.enabled = enabled;
        this.smtpHost = smtpHost;
        this.senderAddress = senderAddress;
        this.resetUrl = resetUrl;
    }

    public void assertConfigured() {
        if (!enabled || smtpHost.isBlank() || senderAddress.isBlank() || resetUrl.isBlank()
                || mailSenders.getIfAvailable() == null) {
            throw new AuthException(
                    "PASSWORD_RESET_EMAIL_UNAVAILABLE",
                    "Chức năng đặt lại mật khẩu chưa được cấu hình SMTP relay. Vui lòng liên hệ quản trị viên.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public boolean sendResetLink(String recipient, String displayName, String token) {
        assertConfigured();
        String link = UriComponentsBuilder.fromUriString(resetUrl)
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(recipient);
        message.setSubject("Đặt lại mật khẩu LMS Platform");
        message.setText("""
                Xin chào %s,

                Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản LMS Platform của bạn.
                Dùng liên kết dưới đây để tạo mật khẩu mới. Liên kết chỉ dùng được một lần và hết hạn sau 30 phút:

                %s

                Nếu bạn không yêu cầu thay đổi mật khẩu, hãy bỏ qua email này.
                """.formatted(displayName, link));
        try {
            mailSenders.getObject().send(message);
            logger.info("Password reset email accepted by the configured SMTP relay.");
            return true;
        } catch (MailException exception) {
            logger.error("Unable to send a password reset email; the reset token was invalidated.", exception);
            return false;
        }
    }
}
