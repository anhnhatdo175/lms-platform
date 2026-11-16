package com.example.lms.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PrerequisiteRequest(
        @NotNull PrerequisiteConditionType conditionType,
        @Min(1) Long prerequisiteLessonId,
        @Min(1) Long prerequisiteExamId) {
}
