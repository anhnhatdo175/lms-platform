package com.example.lms.ai.practice;

import java.time.Instant;

import com.example.lms.ai.session.AdaptiveSessionViews.TaskView;

public final class AttemptViews {

    private AttemptViews() {
    }

    public record AnswerPayload(String selectedOptionId, String text) {
    }

    public record SubmitRequest(AnswerPayload answer, Long responseTimeMs, String clientEventId) {
    }

    public record AttemptResult(long id, long taskId, Boolean isCorrect, double score, String feedback,
            String correctAnswer, String explanation, boolean profileUpdated, Integer profileVersionBefore,
            Integer profileVersionAfter, Instant submittedAt, int hintsUsed, boolean duplicate) {
    }

    public record AttemptHistory(AttemptResult attempt, TaskView task) {
    }

    public record Submitted(AttemptResult result, boolean created) {
    }
}
