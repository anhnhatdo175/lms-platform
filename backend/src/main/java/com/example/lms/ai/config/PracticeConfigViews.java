package com.example.lms.ai.config;

import java.time.Instant;
import java.util.List;

import com.example.lms.ai.AdaptiveTaskType;
import com.example.lms.contract.question.Difficulty;

public final class PracticeConfigViews {

    private PracticeConfigViews() {
    }

    public record Request(Boolean enabled, String subjectCode, String gradeLevel, List<String> allowedSkills,
            Difficulty minDifficulty, Difficulty maxDifficulty, List<AdaptiveTaskType> allowedTaskTypes,
            Integer maxTasksPerSession, Boolean socraticEnabled, Integer maxHintsPerTask) {
    }

    public record View(long id, long courseId, Long lessonId, boolean enabled, String subjectCode, String gradeLevel,
            List<String> allowedSkills, Difficulty minDifficulty, Difficulty maxDifficulty,
            List<AdaptiveTaskType> allowedTaskTypes, int maxTasksPerSession, boolean socraticEnabled,
            int maxHintsPerTask, String effectiveSubjectCode, String effectiveGradeLevel, long updatedBy,
            Instant updatedAt) {
    }
}
