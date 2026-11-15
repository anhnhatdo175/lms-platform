package com.example.lms.question.dto;

import java.util.List;

import com.example.lms.contract.question.Difficulty;
import com.example.lms.contract.question.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateQuestionRequest(
        @NotBlank String content,
        @NotNull QuestionType questionType,
        @NotNull Difficulty difficulty,
        @Size(max = 150) String skillCode,
        String explanation,
        @NotNull @Size(min = 2) List<@Valid @NotNull QuestionOptionRequest> options) {
}
