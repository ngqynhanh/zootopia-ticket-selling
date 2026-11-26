package com.zoopass.auth;

import com.zoopass.common.exception.ApiException;
import com.zoopass.common.exception.ErrorCode;
import com.zoopass.config.AppProperties;
import com.zoopass.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, checks and revokes {@link UserToken}s.
 *
 * <p>Tokens are 256 random bits (URL-safe Base64). Refresh tokens are <b>rotated</b>: each refresh
 * consumes the old token and issues a new one. If an already-used refresh token is presented again,
 * it was probably stolen, so all refresh tokens of that user are revoked.
 */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserTokenRepository repository;
    private final AppProperties.Security settings;
    private final Clock clock;

    public TokenService(UserTokenRepository repository, AppProperties properties, Clock clock) {
        this.repository = repository;
        this.settings = properties.security();
        this.clock = clock;
    }

    /** Creates a token and returns the raw value to send to the user (it is not stored). */
    @Transactional
    public String create(User user, TokenPurpose purpose) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        repository.save(new UserToken(user, hash(raw), purpose, now, now.plus(ttl(purpose))));
        return raw;
    }

    /**
     * Consumes a one-time token and returns its user.
     *
     * @throws ApiException INVALID_TOKEN when unknown, of another purpose, used or expired
     */
    @Transactional
    public User consume(String raw, TokenPurpose purpose) {
        UserToken token = find(raw, purpose);
        Instant now = clock.instant();
        if (!token.isUsable(now)) {
            throw invalid(purpose);
        }
        token.markUsed(now);
        return token.getUser();
    }

    /**
     * Rotates a refresh token: consumes it and returns its user; the caller then issues a new one.
     * Reuse of an already-rotated token revokes every refresh token of the user.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public User rotateRefreshToken(String raw) {
        UserToken token = find(raw, TokenPurpose.REFRESH);
        Instant now = clock.instant();
        if (token.isUsed()) {
            repository.revokeAll(token.getUser().getId(), TokenPurpose.REFRESH, now);
            throw invalid(TokenPurpose.REFRESH);
        }
        if (!token.isUsable(now)) {
            throw invalid(TokenPurpose.REFRESH);
        }
        token.markUsed(now);
        return token.getUser();
    }

    /** Revokes one refresh token if it exists (logout); unknown tokens are ignored. */
    @Transactional
    public void revokeRefreshToken(String raw) {
        repository.findByTokenHash(hash(raw)).ifPresent(token -> {
            if (!token.isUsed()) {
                token.markUsed(clock.instant());
            }
        });
    }

    @Transactional
    public void revokeAll(User user, TokenPurpose purpose) {
        repository.revokeAll(user.getId(), purpose, clock.instant());
    }

    public Duration ttl(TokenPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> settings.verificationTokenTtl();
            case PASSWORD_RESET -> settings.passwordResetTokenTtl();
            case REFRESH -> settings.refreshTokenTtl();
        };
    }

    private UserToken find(String raw, TokenPurpose purpose) {
        if (raw == null || raw.isBlank()) {
            throw invalid(purpose);
        }
        return repository.findByTokenHash(hash(raw))
                .filter(token -> token.getPurpose() == purpose)
                .orElseThrow(() -> invalid(purpose));
    }

    private static ApiException invalid(TokenPurpose purpose) {
        return purpose == TokenPurpose.REFRESH
                ? new ApiException(ErrorCode.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại")
                : new ApiException(ErrorCode.INVALID_TOKEN, "Liên kết không hợp lệ hoặc đã hết hạn");
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
