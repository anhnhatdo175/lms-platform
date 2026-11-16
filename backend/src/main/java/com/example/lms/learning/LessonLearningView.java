package com.example.lms.learning;

import com.example.lms.course.CompletionMethod;
import com.example.lms.course.LessonProgressStatus;
import com.example.lms.course.LessonResourceSummary;
import com.example.lms.course.LessonType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record LessonLearningView(
        Long id,
        Long courseId,
        Long sectionId,
        String title,
        String description,
        String content,
        LessonType lessonType,
        int position,
        String videoUrl,
        Instant videoUrlExpiresAt,
        Integer durationSeconds,
        boolean mandatory,
        CompletionMethod completionMethod,
        BigDecimal completionThreshold,
        Long examId,
        List<LessonResourceSummary> resources,
        Progress progress) {

    public record Progress(
            LessonProgressStatus status,
            int progressSeconds,
            BigDecimal progressPercent,
            Instant completedAt) {
    }
}
