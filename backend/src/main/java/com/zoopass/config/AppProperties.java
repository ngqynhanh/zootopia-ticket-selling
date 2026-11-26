package com.zoopass.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed view of the {@code app.*} settings in application.yml.
 *
 * @param cors        allowed frontend origins
 * @param errors      error response options
 * @param frontendUrl base URL of the React app, used in email links
 * @param mailFrom    sender address of outgoing emails
 * @param security    token lifetimes, cookie and login-limit settings
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Errors errors, String frontendUrl, String mailFrom, Security security) {

    /** @param allowedOrigins origins allowed to call the API from a browser */
    public record Cors(List<String> allowedOrigins) {
    }

    /** @param includeDebugDetail add exception detail to error bodies (dev only) */
    public record Errors(boolean includeDebugDetail) {
    }

    /**
     * @param jwtSecret                HMAC key for access tokens, at least 32 bytes (env JWT_SECRET)
     * @param accessTokenTtl           lifetime of a JWT access token
     * @param refreshTokenTtl          lifetime of a refresh token (HttpOnly cookie)
     * @param verificationTokenTtl     lifetime of an email-verification link
     * @param passwordResetTokenTtl    lifetime of a password-reset link
     * @param refreshCookieSecure      send the refresh cookie over HTTPS only (true in production)
     * @param refreshCookieSameSite    Lax when frontend and API share a site, None when they are on different domains
     * @param bcryptStrength           BCrypt cost factor (10-12)
     * @param loginMaxFailures         failed logins allowed per email+IP inside the window
     * @param loginLockDuration        window / lock time after too many failures
     */
    public record Security(
            String jwtSecret,
            Duration accessTokenTtl,
            Duration refreshTokenTtl,
            Duration verificationTokenTtl,
            Duration passwordResetTokenTtl,
            boolean refreshCookieSecure,
            String refreshCookieSameSite,
            int bcryptStrength,
            int loginMaxFailures,
            Duration loginLockDuration) {
    }
}
