package com.zoopass.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on Spring Data JPA auditing so {@code created_at/updated_at/created_by/updated_by}
 * are filled automatically on every save.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "currentUserAuditor")
public class JpaAuditingConfig {

    /**
     * Returns the id of the user performing the change.
     * Week 1: there is no authentication yet, so changes are recorded as "system" (null).
     * Week 2: read the user id from Spring Security's SecurityContext.
     */
    @Bean
    public AuditorAware<Long> currentUserAuditor() {
        return Optional::empty;
    }
}
