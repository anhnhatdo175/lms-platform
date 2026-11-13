package com.example.lms.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Thu hồi phải có lý do (Spec 12.3). */
public record RevokeEnrollmentRequest(@NotBlank @Size(max = 500) String reason) {
}
