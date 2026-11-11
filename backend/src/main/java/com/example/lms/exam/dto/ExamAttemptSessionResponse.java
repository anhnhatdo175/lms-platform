package com.example.lms.exam.dto;

import java.math.BigDecimal;
import java.util.List;

import com.example.lms.contract.question.QuestionType;

/**
 * OpenAPI {@code ExamAttemptSession}: đề snapshot và đáp án đã lưu. Không có cờ đáp án đúng hay giải thích:
 * response này dựng từ record riêng, không serialize thẳng snapshot.
 */
public record ExamAttemptSessionResponse(ExamAttemptResponse attempt, List<AttemptQuestionView> questions) {

    public record AttemptQuestionView(long id, int position, QuestionType questionType, String content,
            List<OptionView> options, BigDecimal maximumScore, List<Long> selectedOptionIds) {
    }

    public record OptionView(long id, String content) {
    }
}
