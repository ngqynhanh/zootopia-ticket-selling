package com.zoopass.notification;

/**
 * Sends plain-text emails. Two implementations:
 * {@link SmtpEmailService} when SMTP is configured, {@link LoggingEmailService} otherwise (development),
 * which writes the email, including its link, to the log.
 */
public interface EmailService {

    void send(String to, String subject, String body);
}
