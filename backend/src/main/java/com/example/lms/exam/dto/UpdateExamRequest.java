package com.example.lms.exam.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.lms.exam.ResultDisplayMode;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

/** OpenAPI {@code UpdateExamRequest}; trường bỏ trống giữ nguyên giá trị cũ. */
public record UpdateExamRequest(String title, String description, @Min(1) Integer durationMinutes,
        @DecimalMin("0") @DecimalMax("100") BigDecimal passingScore, @Min(1) Integer maximumAttempts,
        Boolean shuffleQuestions, Boolean shuffleOptions, ResultDisplayMode resultDisplayMode, Instant startAt,
        Instant endAt) {
}
