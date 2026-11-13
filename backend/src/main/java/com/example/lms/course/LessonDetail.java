package com.example.lms.course;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record LessonDetail(
        Long id,
        Long sectionId,
        String title,
        String description,
        String content,
        LessonType lessonType,
        int position,
        Integer durationSeconds,
        Long videoResourceId,
        boolean preview,
        boolean published,
        boolean mandatory,
        CompletionMethod completionMethod,
        BigDecimal completionThreshold,
        LessonPrerequisiteRef prerequisite,
        Long examId,
        List<LessonResourceSummary> resources,
        Instant createdAt,
        Instant updatedAt) {

    public static LessonDetail from(Lesson lesson) {
        return new LessonDetail(
                lesson.getId(),
                lesson.getSection().getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getContent(),
                lesson.getLessonType(),
                lesson.getPosition(),
                lesson.getDurationSeconds(),
                lesson.getVideoResourceId(),
                lesson.isPreview(),
                lesson.isPublished(),
                lesson.isMandatory(),
                lesson.getCompletionMethod(),
                lesson.getCompletionThreshold(),
                null,
                null,
                List.of(),
                lesson.getCreatedAt(),
                lesson.getUpdatedAt());
    }
}
