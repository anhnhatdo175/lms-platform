package com.example.lms.enrollment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.lms.enrollment.EnrollmentSource;
import com.example.lms.enrollment.EnrollmentStatus;

/** OpenAPI {@code EnrollmentSummary}; {@code status} là trạng thái hiệu lực tại thời điểm trả về. */
public record EnrollmentSummaryResponse(long id, long courseId, String courseSlug, String courseTitle,
        EnrollmentStatus status, Instant enrolledAt, BigDecimal progressPercent, Instant expiresAt,
        EnrollmentSource source) {
}
