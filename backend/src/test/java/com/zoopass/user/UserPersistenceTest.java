package com.zoopass.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.zoopass.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** JPA mapping, auditing and soft delete on a real database. */
@IntegrationTest
@Transactional
class UserPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void auditColumnsAreFilledOnSave() {
        User saved = userRepository.saveAndFlush(
                new User("  Staff.One@ZooPass.vn ", "$2a$10$hash", "Trần Thị Bình", Role.STAFF));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("staff.one@zoopass.vn");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.isActive()).isTrue();
        assertThat(userRepository.findByEmailIgnoreCase("STAFF.ONE@zoopass.vn")).isPresent();
    }

    @Test
    void softDeleteKeepsTheRow() {
        User user = userRepository.saveAndFlush(
                new User("customer.two@zoopass.vn", "$2a$10$hash", "Lê Văn Cường", Role.CUSTOMER));

        user.softDelete();
        userRepository.saveAndFlush(user);

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.isActive()).isFalse();
        assertThat(reloaded.getDeletedAt()).isNotNull();
    }
}
