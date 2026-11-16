package com.example.lms.course;

import java.time.Instant;
import java.util.List;

public record CourseDetail(
        Long id,
        String slug,
        String title,
        String shortDescription,
        String description,
        CategorySummary category,
        String thumbnailUrl,
        List<TeacherSummary> teachers,
        List<SectionDetail> sections,
        Instant createdAt,
        long originalPrice,
        long salePrice,
        String subjectCode,
        String gradeLevel,
        Integer accessDurationDays,
        CourseCompletionRule completionRule,
        Instant publishedAt) {

    public record TeacherSummary(Long id, String displayName) {
    }

    public record SectionDetail(
            Long id,
            String title,
            String description,
            int position,
            List<LessonSummary> lessons,
            int releaseAfterDays) {
    }

    public record LessonSummary(
            Long id,
            String title,
            LessonType lessonType,
            int position,
            Integer durationSeconds,
            boolean preview,
            boolean mandatory,
            boolean published) {
    }
}
