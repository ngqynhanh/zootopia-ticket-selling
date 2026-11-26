package com.zoopass.config;

import com.zoopass.security.CurrentUser;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Turns on Spring Data JPA auditing so {@code created_at/updated_at/created_by/updated_by}
 * are filled automatically on every save.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "currentUserAuditor")
public class JpaAuditingConfig {

    /**
     * Id of the logged-in user making the change, or empty (stored as NULL = system) for
     * anonymous requests such as self-registration and for scheduled jobs.
     */
    @Bean
    public AuditorAware<Long> currentUserAuditor() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CurrentUser user) {
                return Optional.of(user.id());
            }
            return Optional.empty();
        };
    }
}
