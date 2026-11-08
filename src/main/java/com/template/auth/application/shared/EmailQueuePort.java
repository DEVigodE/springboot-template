package com.template.auth.application.shared;

/** Durable, asynchronous e-mail delivery (the worker calls EmailSenderPort). */
public interface EmailQueuePort {

    record EmailJob(String to, String subject, String body) {
    }

    void enqueue(EmailJob job);
}
