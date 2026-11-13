package com.example.lms.enrollment.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** OpenAPI {@code GrantEnrollmentRequest}; {@code expiresAt} trống thì dùng {@code access_duration_days} của Course. */
public record GrantEnrollmentRequest(@NotNull @Positive Long studentId, @NotNull @Positive Long courseId,
        Instant expiresAt, @NotBlank @Size(max = 500) String reason) {
}
