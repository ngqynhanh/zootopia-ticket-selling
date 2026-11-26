package com.zoopass.security;

import com.zoopass.config.AppProperties;
import com.zoopass.user.UserRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * HTTP security: stateless JWT authentication, URL rules, CORS and security headers.
 *
 * <p>Fine-grained rules (which role may call which endpoint) are declared next to each endpoint
 * with {@code @PreAuthorize}, enabled by {@link EnableMethodSecurity}.
 *
 * <p>CSRF protection is disabled because the API authenticates with the {@code Authorization} header,
 * which a malicious site cannot make the browser send. The only cookie (refresh token) is scoped to
 * {@code /api/v1/auth}, is HttpOnly and SameSite, and its endpoints also check the Origin header.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   UserRepository userRepository,
                                                   SecurityErrorHandlers errorHandlers) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; object-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandlers)
                        .accessDeniedHandler(errorHandlers))
                .authorizeHttpRequests(auth -> auth
                        // Registration, login, token refresh, password reset
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Public read-only data for guests
                        .requestMatchers(HttpMethod.GET, "/api/v1/ticket-types/**", "/api/v1/combos/**",
                                "/api/v1/settings/public").permitAll()
                        // Health check, API documentation, error page
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/swagger-ui.html",
                                "/swagger-ui/**", "/api-docs/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** BCrypt with a configurable cost factor (salt is generated per password). */
    @Bean
    public PasswordEncoder passwordEncoder(AppProperties properties) {
        return new BCryptPasswordEncoder(properties.security().bcryptStrength());
    }

    /** Only the configured frontend origins may call {@code /api/**} from a browser. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Request-Id", "Location"));
        // Needed so the browser sends the HttpOnly refresh-token cookie.
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /** Injectable clock so token expiry and lockouts can be tested. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
