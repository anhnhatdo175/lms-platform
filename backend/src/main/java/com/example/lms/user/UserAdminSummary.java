package com.example.lms.user;

import java.time.Instant;
import java.util.List;

public record UserAdminSummary(
        Long id,
        String email,
        String displayName,
        Long avatarResourceId,
        UserStatus status,
        List<UserRole> roles,
        Instant createdAt,
        Instant lastLoginAt) {

    public static UserAdminSummary from(User user) {
        return new UserAdminSummary(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarFileId(),
                user.getStatus(),
                user.getRoles().stream().sorted().toList(),
                user.getCreatedAt(),
                user.getLastLoginAt());
    }
}
