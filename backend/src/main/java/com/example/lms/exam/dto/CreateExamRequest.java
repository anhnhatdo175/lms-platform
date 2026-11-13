package com.example.lms.exam.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.lms.exam.ResultDisplayMode;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** OpenAPI {@code CreateExamRequest}. */
public record CreateExamRequest(@NotBlank String title, String description, @NotNull @Min(1) Integer durationMinutes,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal passingScore, @Min(1) Integer maximumAttempts,
        Boolean shuffleQuestions, Boolean shuffleOptions, ResultDisplayMode resultDisplayMode, Instant startAt,
        Instant endAt) {
}
