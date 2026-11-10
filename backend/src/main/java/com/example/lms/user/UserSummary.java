package com.example.lms.user;

import java.time.Instant;

public record UserSummary(
        Long id,
        String fullName,
        String email,
        UserRole role,
        UserStatus status,
        Instant createdAt) {

    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
