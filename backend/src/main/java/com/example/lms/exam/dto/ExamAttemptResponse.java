package com.example.lms.exam.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.lms.exam.AttemptStatus;
import com.example.lms.exam.ExamAttempt;
import com.example.lms.exam.SubmissionType;

/**
 * OpenAPI {@code ExamAttempt}. {@code serverTime} để frontend dựng đồng hồ theo giờ server; điểm chỉ hiện khi chế
 * độ hiển thị kết quả cho phép.
 */
public record ExamAttemptResponse(long id, long examId, int attemptNumber, AttemptStatus status,
        SubmissionType submissionType, Instant startedAt, Instant expiresAt, Instant submittedAt, Instant serverTime,
        BigDecimal score, BigDecimal maximumScore, BigDecimal passingScore, Boolean passed, long version) {

    public static ExamAttemptResponse from(ExamAttempt a, Instant serverTime, boolean scoreVisible) {
        return new ExamAttemptResponse(a.getId(), a.getExamId(), a.getAttemptNumber(), a.getStatus(),
                a.getSubmissionType(), a.getStartedAt(), a.getExpiresAt(), a.getSubmittedAt(), serverTime,
                scoreVisible ? a.getScore() : null, a.getMaximumScore(), a.getPassingScore(),
                scoreVisible ? a.getPassed() : null, a.getVersion());
    }
}
