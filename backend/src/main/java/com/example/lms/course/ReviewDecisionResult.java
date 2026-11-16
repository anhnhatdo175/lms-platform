package com.example.lms.course;

import java.time.Instant;

public record ReviewDecisionResult(
        Long courseId,
        CourseStatus status,
        Long reviewerId,
        String reason,
        Instant updatedAt) {

    public static ReviewDecisionResult from(Course course, Long reviewerId, String reason) {
        return new ReviewDecisionResult(
                course.getId(),
                course.getStatus(),
                reviewerId,
                reason,
                course.getUpdatedAt());
    }
}
