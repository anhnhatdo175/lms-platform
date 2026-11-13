package com.example.lms.course;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateLessonRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull LessonType lessonType,
        @Size(max = 65535) String description,
        String content,
        @Min(1) Long videoResourceId,
        @Min(0) Integer durationSeconds,
        Boolean preview,
        Boolean published,
        Boolean mandatory,
        CompletionMethod completionMethod,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal completionThreshold,
        @Min(1) Integer position) {
}
