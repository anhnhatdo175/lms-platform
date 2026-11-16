package com.example.lms.question.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** OpenAPI {@code QuestionOptionInput}. */
public record QuestionOptionRequest(@NotBlank String content, @NotNull Boolean correct, @Min(1) Integer position) {
}
