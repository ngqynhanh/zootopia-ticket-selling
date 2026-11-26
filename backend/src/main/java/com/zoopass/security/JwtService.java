package com.zoopass.security;

import com.zoopass.config.AppProperties;
import com.zoopass.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies short-lived JWT access tokens (HMAC-SHA256).
 *
 * <p>The token carries only the user id (subject), email and role. It is not stored on the server;
 * the signature proves it was issued by us and the expiry limits the damage if it leaks.
 * Long sessions are handled by the refresh token (an opaque, revocable value in the database).
 */
@Service
public class JwtService {

    private static final String ISSUER = "zoopass";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration accessTokenTtl;
    private final Clock clock;

    public JwtService(AppProperties properties, Clock clock) {
        String secret = properties.security().jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            // Fail at startup instead of running with a weak or missing key.
            throw new IllegalStateException("JWT_SECRET must be set to at least " + MIN_SECRET_BYTES + " bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = properties.security().accessTokenTtl();
        this.clock = clock;
    }

    public String issueAccessToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    /**
     * Returns the user id if the token has a valid signature, issuer and expiry; empty otherwise.
     * Invalid tokens are not errors here: the request simply continues as anonymous.
     */
    public Optional<Long> parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(Long.valueOf(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
