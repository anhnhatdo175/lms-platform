package com.example.lms.exam.dto;

import java.time.Instant;
import java.util.List;

/** OpenAPI {@code ExamAnswerAck}. */
public record ExamAnswerAck(long attemptQuestionId, List<Long> selectedOptionIds, Instant savedAt,
        long attemptVersion) {
}
