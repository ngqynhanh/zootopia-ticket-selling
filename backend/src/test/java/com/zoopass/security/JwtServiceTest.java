package com.zoopass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zoopass.config.AppProperties;
import com.zoopass.user.Role;
import com.zoopass.user.User;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Unit tests: no Spring context, a fixed clock to test expiry. */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef0123";
    private static final Instant NOW = Instant.parse("2026-11-20T08:00:00Z");

    @Test
    void issuedTokenRoundTripsToTheUserId() throws Exception {
        JwtService service = service(SECRET, Clock.fixed(NOW, ZoneOffset.UTC));
        String token = service.issueAccessToken(user(42L));

        assertThat(service.parseUserId(token)).contains(42L);
        assertThat(service.accessTokenTtlSeconds()).isEqualTo(900);
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        String token = service(SECRET, Clock.fixed(NOW, ZoneOffset.UTC)).issueAccessToken(user(1L));
        JwtService sixteenMinutesLater = service(SECRET, Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));

        assertThat(sixteenMinutesLater.parseUserId(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        String token = service("another-secret-0123456789abcdef01234", Clock.systemUTC()).issueAccessToken(user(1L));

        assertThat(service(SECRET, Clock.systemUTC()).parseUserId(token)).isEmpty();
        assertThat(service(SECRET, Clock.systemUTC()).parseUserId("not-a-jwt")).isEmpty();
    }

    @Test
    void refusesToStartWithAShortSecret() {
        assertThatThrownBy(() -> service("too-short", Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    private static JwtService service(String secret, Clock clock) {
        var security = new AppProperties.Security(secret, Duration.ofMinutes(15), Duration.ofDays(7),
                Duration.ofHours(24), Duration.ofMinutes(30), false, "Lax", 4, 5, Duration.ofMinutes(15));
        return new JwtService(new AppProperties(null, null, null, null, security), clock);
    }

    private static User user(long id) throws Exception {
        User user = new User("unit@zoopass.test", "hash", "Unit Test", Role.CUSTOMER);
        Field idField = com.zoopass.common.entity.BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }
}
