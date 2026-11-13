package com.example.lms.exam.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** OpenAPI {@code AddExamQuestionRequest}; {@code position} trống thì thêm vào cuối. */
public record AddExamQuestionRequest(@NotNull @Positive Long questionId, @Min(1) Integer position,
        @NotNull @DecimalMin("0") BigDecimal score, Boolean required) {
}
