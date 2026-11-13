package com.example.lms.course;

import java.time.Instant;

public record CourseManaged(
        Long id,
        String slug,
        String title,
        String shortDescription,
        String description,
        CategorySummary category,
        String thumbnailUrl,
        CourseStatus status,
        Instant createdAt,
        Instant updatedAt,
        long originalPrice,
        long salePrice,
        String subjectCode,
        String gradeLevel,
        Integer accessDurationDays,
        CourseCompletionRule completionRule,
        Instant publishedAt,
        String lastReviewNote) {

    public static CourseManaged from(Course course, String thumbnailUrl, String lastReviewNote) {
        return new CourseManaged(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDescription(),
                CategorySummary.from(course.getCategory()),
                thumbnailUrl,
                course.getStatus(),
                course.getCreatedAt(),
                course.getUpdatedAt(),
                course.getOriginalPrice(),
                course.getSalePrice(),
                course.getSubjectCode(),
                course.getGradeLevel(),
                course.getAccessDurationDays(),
                course.getCompletionRule(),
                course.getPublishedAt(),
                lastReviewNote);
    }
}
