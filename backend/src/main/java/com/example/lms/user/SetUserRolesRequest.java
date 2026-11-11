package com.example.lms.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SetUserRolesRequest(@NotEmpty List<@NotNull UserRole> roles) {
}
