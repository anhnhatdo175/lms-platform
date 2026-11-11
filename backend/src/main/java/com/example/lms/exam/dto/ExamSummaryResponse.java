package com.example.lms.exam.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.lms.exam.Exam;
import com.example.lms.exam.ExamStatus;
import com.example.lms.exam.ResultDisplayMode;

/** OpenAPI {@code ExamSummary}. */
public record ExamSummaryResponse(long id, long lessonId, String title, String description, int durationMinutes,
        BigDecimal passingScore, Integer maximumAttempts, boolean shuffleQuestions, boolean shuffleOptions,
        ResultDisplayMode resultDisplayMode, Instant startAt, Instant endAt, int questionCount, ExamStatus status,
        BigDecimal maximumScore) {

    public static ExamSummaryResponse from(Exam exam, int questionCount, BigDecimal maximumScore) {
        return new ExamSummaryResponse(exam.getId(), exam.getLessonId(), exam.getTitle(), exam.getDescription(),
                exam.getDurationMinutes(), exam.getPassingScore(), exam.getMaximumAttempts(), exam.isShuffleQuestions(),
                exam.isShuffleOptions(), exam.getResultDisplayMode(), exam.getStartAt(), exam.getEndAt(), questionCount,
                exam.getStatus(), maximumScore);
    }
}
