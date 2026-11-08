package com.template.auth.infrastructure.email;

import com.template.auth.application.shared.EmailSenderPort;
import com.template.auth.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * EmailSenderPort adapter over Spring's JavaMailSender (any SMTP provider).
 * Requires EMAIL_SENDER=smtp plus the SPRING_MAIL_* properties; EMAIL_FROM must
 * be a sender the provider accepts.
 */
@Component
@ConditionalOnProperty(name = "app.email.sender", havingValue = "smtp")
public class SmtpEmailSender implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.from = properties.email().from();
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
