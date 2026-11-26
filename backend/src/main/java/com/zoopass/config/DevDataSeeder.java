package com.zoopass.config;

import com.zoopass.user.Role;
import com.zoopass.user.User;
import com.zoopass.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Development only ({@code dev} profile): creates one verified account per role so every feature can
 * be tried locally. Production demo accounts come from database/seed-data.sql.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private record Account(String email, String password, String fullName, Role role) {
    }

    private static final Account[] ACCOUNTS = {
        new Account("admin@zoopass.test", "Admin123!", "Quản trị viên", Role.ADMIN),
        new Account("manager@zoopass.test", "Manager123!", "Quản lý", Role.MANAGER),
        new Account("staff@zoopass.test", "Staff123!", "Nhân viên quầy", Role.STAFF),
        new Account("customer@zoopass.test", "Customer123!", "Khách hàng", Role.CUSTOMER),
    };

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Account account : ACCOUNTS) {
            if (!userRepository.existsByEmailIgnoreCase(account.email())) {
                User user = new User(account.email(), passwordEncoder.encode(account.password()),
                        account.fullName(), account.role());
                user.markEmailVerified();
                userRepository.save(user);
                log.info("Dev account created: {} ({})", account.email(), account.role());
            }
        }
    }
}
