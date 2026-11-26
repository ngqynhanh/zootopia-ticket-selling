package com.zoopass.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Beans replaced in integration tests. */
@TestConfiguration
public class TestBeans {

    /** @Primary: injected wherever an EmailService is needed, instead of the logging/SMTP one. */
    @Bean
    @Primary
    public CapturingEmailService capturingEmailService() {
        return new CapturingEmailService();
    }
}
