package com.template.auth.infrastructure.email;

import com.template.auth.application.shared.EmailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default EmailSenderPort adapter: logs instead of calling a real provider.
 * Fine for dev/test; set EMAIL_SENDER=smtp for SmtpEmailSender.
 */
@Component
@ConditionalOnProperty(name = "app.email.sender", havingValue = "log", matchIfMissing = true)
public class LogEmailSender implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("Enviando e-mail para {} - \"{}\": {}", to, subject, body);
    }
}
