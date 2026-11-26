package com.zoopass.user.dto;

import com.zoopass.user.Role;
import com.zoopass.user.User;
import java.time.Instant;
import java.time.LocalDate;

/** Public view of a user account. Never contains the password hash. */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        LocalDate dateOfBirth,
        String avatarUrl,
        Role role,
        boolean emailVerified,
        boolean active,
        Instant lastLoginAt,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getDateOfBirth(),
                user.getAvatarUrl(),
                user.getRole(),
                user.isEmailVerified(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
