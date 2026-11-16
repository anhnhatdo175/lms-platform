package com.example.lms.course;

import java.time.Instant;

public record CourseSummary(
        Long id,
        String slug,
        String title,
        String shortDescription,
        String thumbnailUrl,
        CourseStatus status,
        Instant createdAt,
        long originalPrice,
        long salePrice,
        CategorySummary category,
        String subjectCode,
        String gradeLevel,
        Instant publishedAt) {

    public static CourseSummary from(Course course, String thumbnailUrl) {
        return new CourseSummary(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getShortDescription(),
                thumbnailUrl,
                course.getStatus(),
                course.getCreatedAt(),
                course.getOriginalPrice(),
                course.getSalePrice(),
                CategorySummary.from(course.getCategory()),
                course.getSubjectCode(),
                course.getGradeLevel(),
                course.getPublishedAt());
    }
}
