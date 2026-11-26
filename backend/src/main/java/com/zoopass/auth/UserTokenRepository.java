package com.zoopass.auth;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link UserToken}. */
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenHash(String tokenHash);

    /** Marks every still-unused token of this purpose as used (revocation). */
    @Modifying
    @Query("""
            UPDATE UserToken t SET t.usedAt = :now
            WHERE t.user.id = :userId AND t.purpose = :purpose AND t.usedAt IS NULL
            """)
    int revokeAll(@Param("userId") Long userId, @Param("purpose") TokenPurpose purpose, @Param("now") Instant now);
}
