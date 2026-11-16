package com.example.lms.ai.session;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.example.lms.ai.AdaptiveTaskType;
import com.example.lms.ai.SessionErrorCode;
import com.example.lms.ai.SessionStatus;
import com.example.lms.ai.TaskOrigin;
import com.example.lms.contract.question.Difficulty;

/** DTO của OpenAPI cho phía Student. Task KHÔNG mang đáp án/giải thích (AC-AI-12). */
public final class AdaptiveSessionViews {

    private AdaptiveSessionViews() {
    }

    public record CreateRequest(Long courseId, Long lessonId, List<String> targetTopics, Integer taskCount,
            List<AdaptiveTaskType> taskTypes, String clientRequestId) {
    }

    public record Evidence(long documentId, String chunkId, String title, String excerpt) {
    }

    public record TaskView(long id, AdaptiveTaskType taskType, String prompt, List<Map<String, String>> options,
            Difficulty difficulty, List<String> skillCodes, List<Evidence> evidence, TaskOrigin origin,
            String personalizationReason, int hintsAvailable, boolean attempted) {
    }

    public record SessionView(long id, SessionStatus status, Long courseId, Long lessonId, int requestedTaskCount,
            List<TaskView> tasks, Integer profileVersionUsed, String pipelineVersion, SessionErrorCode errorCode,
            Instant createdAt, Instant completedAt, Long configId, Map<String, Object> constraints, int autoRetryCount,
            int manualRetryCount, Instant nextRetryAt) {
    }

    public record PracticeOptions(boolean configured, boolean enabled, long courseId, Long lessonId, String subjectCode,
            String gradeLevel, List<String> allowedSkills, Difficulty minDifficulty, Difficulty maxDifficulty,
            List<AdaptiveTaskType> allowedTaskTypes, Integer maxTasksPerSession, boolean socraticEnabled,
            Integer maxHintsPerTask) {
    }
}
