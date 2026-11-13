package com.example.lms.exam.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** OpenAPI {@code ExamAnswerRequest}; mảng rỗng để bỏ chọn; {@code version} là token autosave của Attempt. */
public record ExamAnswerRequest(@NotNull List<@NotNull @Positive Long> selectedOptionIds, @Min(0) Long version) {
}
