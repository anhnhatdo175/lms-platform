package com.example.lms.auth;

import com.example.lms.user.User;
import com.example.lms.user.UserRole;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) {
    public static AuthResponse from(AuthenticatedSession session) {
        User user = session.user();
        return new AuthResponse(
                session.accessToken(),
                "Bearer",
                session.expiresInSeconds(),
                new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole()));
    }

    public record UserSummary(Long id, String fullName, String email, UserRole role) {
    }
}
