package com.zoopass.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Sends through the SMTP server configured by {@code SPRING_MAIL_HOST / _PORT / _USERNAME / _PASSWORD}.
 * A failed send is logged instead of failing the request: the user can ask for the email again.
 */
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailService(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            log.info("Email '{}' sent to {}", subject, to);
        } catch (MailException ex) {
            log.error("Could not send email '{}' to {}: {}", subject, to, ex.getMessage());
        }
    }
}
