package com.example.lms.auth;

import com.example.lms.user.UserSummary;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) {
    public static AuthResponse from(AuthenticatedSession session) {
        return new AuthResponse(
                session.accessToken(),
                "Bearer",
                session.expiresInSeconds(),
                UserSummary.from(session.user()));
    }
}
