package com.template.auth.application.shared;

/** Actual e-mail delivery. Default adapter only logs; swap via EMAIL_SENDER=smtp. */
public interface EmailSenderPort {

    void send(String to, String subject, String body);
}
