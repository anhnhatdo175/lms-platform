package com.example.lms.exam.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.lms.exam.ResultDisplayMode;

/** OpenAPI {@code ExamView}: thông tin trước khi làm bài, không có câu hỏi. */
public record ExamViewResponse(long id, long lessonId, String title, String description, int durationMinutes,
        BigDecimal passingScore, Integer maximumAttempts, int attemptsUsed, ResultDisplayMode resultDisplayMode,
        Instant startAt, Instant endAt, boolean canStart, Long inProgressAttemptId, BigDecimal bestScore,
        Boolean passed) {
}
