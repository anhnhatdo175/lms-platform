package com.example.lms.auth;

import com.example.lms.user.User;
import com.example.lms.user.UserRole;

import java.util.Set;

public record UserPrincipal(Long id, String displayName, String email, Set<UserRole> roles) {
    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getFullName(), user.getEmail(), user.getRoles());
    }
}
