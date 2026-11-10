package com.example.lms.auth;

import com.example.lms.user.User;

public record AuthenticatedSession(User user, String accessToken, long expiresInSeconds, String refreshToken) {
}
