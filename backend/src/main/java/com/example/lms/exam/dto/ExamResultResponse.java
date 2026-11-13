package com.example.lms.exam.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.example.lms.exam.AttemptStatus;
import com.example.lms.exam.ResultDisplayMode;
import com.example.lms.exam.SubmissionType;

/** OpenAPI {@code ExamResult}; {@code answers} là {@code null} khi chế độ hiển thị chưa cho xem chi tiết. */
public record ExamResultResponse(long attemptId, long examId, AttemptStatus status, SubmissionType submissionType,
        BigDecimal score, BigDecimal maximumScore, BigDecimal passingScore, Boolean passed, Instant submittedAt,
        ResultDisplayMode resultDisplayMode, List<AnswerResult> answers) {

    public record AnswerResult(long attemptQuestionId, List<Long> selectedOptionIds, List<Long> correctOptionIds,
            Boolean correct, BigDecimal scoreAwarded, String explanation) {
    }
}
