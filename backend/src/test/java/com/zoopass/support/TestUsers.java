package com.zoopass.support;

import com.zoopass.security.JwtService;
import com.zoopass.user.Role;
import com.zoopass.user.User;
import com.zoopass.user.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates users directly in the database for tests. Emails are unique per call because all test
 * classes share one embedded database (and one Spring context).
 */
public final class TestUsers {

    public static final String PASSWORD = "Secret123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public TestUsers(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@zoopass.test";
    }

    /** A verified, active user with {@link #PASSWORD}. */
    public User create(Role role) {
        User user = new User(uniqueEmail(role.name().toLowerCase()), passwordEncoder.encode(PASSWORD),
                "Test " + role.name(), role);
        user.markEmailVerified();
        return userRepository.saveAndFlush(user);
    }

    public String bearer(User user) {
        return "Bearer " + jwtService.issueAccessToken(user);
    }

    public String bearer(Role role) {
        return bearer(create(role));
    }
}
