package com.zoopass.auth;

import com.zoopass.common.exception.ApiException;
import com.zoopass.common.exception.ErrorCode;
import com.zoopass.config.AppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Brute-force protection for login: after N failed attempts for the same email from the same IP
 * within the window, further attempts are refused with 429 until the window has passed.
 *
 * <p>Kept in memory (sliding window per key). This is enough for one backend instance; with several
 * instances the counters would move to a shared store such as Redis.
 */
@Service
public class LoginAttemptService {

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration window;
    private final Clock clock;

    public LoginAttemptService(AppProperties properties, Clock clock) {
        this.maxFailures = properties.security().loginMaxFailures();
        this.window = properties.security().loginLockDuration();
        this.clock = clock;
    }

    /** @throws ApiException TOO_MANY_REQUESTS while the key is locked */
    public void checkAllowed(String email, String ip) {
        Deque<Instant> attempts = failures.get(key(email, ip));
        if (attempts == null) {
            return;
        }
        synchronized (attempts) {
            prune(attempts);
            if (attempts.size() >= maxFailures) {
                long minutes = Math.max(1, Duration.between(clock.instant(), attempts.peekFirst().plus(window))
                        .toMinutes() + 1);
                throw new ApiException(ErrorCode.TOO_MANY_REQUESTS,
                        "Bạn đã đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + minutes + " phút");
            }
        }
    }

    public void recordFailure(String email, String ip) {
        Deque<Instant> attempts = failures.computeIfAbsent(key(email, ip), k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts);
            attempts.addLast(clock.instant());
        }
    }

    public void reset(String email, String ip) {
        failures.remove(key(email, ip));
    }

    private void prune(Deque<Instant> attempts) {
        Instant limit = clock.instant().minus(window);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(limit)) {
            attempts.pollFirst();
        }
    }

    private static String key(String email, String ip) {
        return email + "|" + ip;
    }
}
