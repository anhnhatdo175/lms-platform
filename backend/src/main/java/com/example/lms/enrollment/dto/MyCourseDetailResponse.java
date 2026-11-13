package com.example.lms.enrollment.dto;

import java.util.List;

/** OpenAPI {@code MyCourseDetail}: {@code progress} và {@code sections} do A cung cấp (interface M2). */
public record MyCourseDetailResponse(EnrollmentSummaryResponse enrollment, Object progress, List<?> sections) {
}
