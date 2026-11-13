package com.example.lms.user;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        UserRole role,
        UserStatus status,
        @Size(max = 120) String fullName) {
}
