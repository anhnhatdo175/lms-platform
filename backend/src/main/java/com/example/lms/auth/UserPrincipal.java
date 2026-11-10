package com.example.lms.auth;

import com.example.lms.user.User;
import com.example.lms.user.UserRole;

public record UserPrincipal(Long id, String fullName, String email, UserRole role) {
    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
