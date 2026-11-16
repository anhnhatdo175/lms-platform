package com.example.lms.course;

import java.time.Instant;

public record ReviewRequestSummary(
        Long id,
        Long courseId,
        String courseTitle,
        Long teacherId,
        String teacherName,
        CourseReviewStatus status,
        Instant submittedAt) {

    public static ReviewRequestSummary from(CourseReviewRequest request) {
        return new ReviewRequestSummary(
                request.getId(),
                request.getCourse().getId(),
                request.getCourse().getTitle(),
                request.getSubmittedBy().getId(),
                request.getSubmittedBy().getFullName(),
                request.getStatus(),
                request.getSubmittedAt());
    }
}
