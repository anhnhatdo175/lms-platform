package com.example.lms.user;

import java.util.List;

public record UserSummary(
        Long id,
        String email,
        String displayName,
        Long avatarResourceId,
        List<UserRole> roles) {

    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarFileId(),
                user.getRoles().stream().sorted().toList());
    }
}
