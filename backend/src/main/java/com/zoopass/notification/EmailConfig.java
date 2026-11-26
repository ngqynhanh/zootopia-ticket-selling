package com.zoopass.notification;

import com.zoopass.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Chooses the email implementation. Spring Boot creates a {@link JavaMailSender} only when
 * {@code spring.mail.host} (env SPRING_MAIL_HOST) is set.
 */
@Configuration
public class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    @Bean
    public EmailService emailService(ObjectProvider<JavaMailSender> mailSender, AppProperties properties) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("No SMTP server configured (SPRING_MAIL_HOST): emails are written to the log only");
            return new LoggingEmailService();
        }
        return new SmtpEmailService(sender, properties.mailFrom());
    }
}
