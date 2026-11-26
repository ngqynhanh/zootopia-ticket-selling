package com.zoopass.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development fallback when no SMTP server is configured: the email (and its verification or reset
 * link) is written to the log so the flow can be tested locally.
 */
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[DEV EMAIL] to={} subject={}\n{}", to, subject, body);
    }
}
