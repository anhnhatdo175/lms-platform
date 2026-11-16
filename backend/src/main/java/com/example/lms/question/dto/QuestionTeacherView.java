package com.example.lms.question.dto;

import java.time.Instant;
import java.util.List;

import com.example.lms.contract.question.Difficulty;
import com.example.lms.contract.question.QuestionType;
import com.example.lms.question.Question;
import com.example.lms.question.QuestionSource;

/** OpenAPI {@code QuestionTeacherView}: có cờ {@code correct}, chỉ dành cho Teacher/Admin của Course. */
public record QuestionTeacherView(long id, long courseId, String content, QuestionType questionType,
        Difficulty difficulty, String skillCode, QuestionSource source, String explanation, boolean active,
        int version, List<OptionView> options, Instant createdAt, Instant updatedAt) {

    public record OptionView(long id, String content, boolean correct, int position) {
    }

    public static QuestionTeacherView from(Question q) {
        return new QuestionTeacherView(q.getId(), q.getCourseId(), q.getContent(), q.getQuestionType(),
                q.getDifficulty(), q.getSkillCode(), q.getSource(), q.getExplanation(), q.isActive(), q.getVersion(),
                q.getOptions().stream()
                        .map(o -> new OptionView(o.getId(), o.getContent(), o.isCorrect(), o.getPosition())).toList(),
                q.getCreatedAt(), q.getUpdatedAt());
    }
}
