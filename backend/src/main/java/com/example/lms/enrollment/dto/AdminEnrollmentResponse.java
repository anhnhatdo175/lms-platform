package com.example.lms.enrollment.dto;

import java.time.Instant;

import com.example.lms.enrollment.Enrollment;
import com.example.lms.enrollment.EnrollmentSource;
import com.example.lms.enrollment.EnrollmentStatus;

/** OpenAPI {@code AdminEnrollment}. */
public record AdminEnrollmentResponse(long id, long studentId, String studentEmail, long courseId, String courseTitle,
        EnrollmentSource source, EnrollmentStatus status, Instant startsAt, Instant expiresAt, Instant revokedAt,
        String revokedReason) {

    public static AdminEnrollmentResponse from(Enrollment e, String studentEmail, String courseTitle) {
        return new AdminEnrollmentResponse(e.getId(), e.getStudentId(), studentEmail, e.getCourseId(), courseTitle,
                e.getSource(), e.getStatus(), e.getStartsAt(), e.getExpiresAt(), e.getRevokedAt(),
                e.getRevokedReason());
    }
}
