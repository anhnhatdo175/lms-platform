package com.example.lms.exam.dto;

import java.math.BigDecimal;
import java.util.List;

import com.example.lms.contract.question.Difficulty;
import com.example.lms.contract.question.QuestionQueryService.QuestionView;
import com.example.lms.contract.question.QuestionType;

/** OpenAPI {@code ExamBuilderView}: dành cho Teacher, có đáp án đúng. */
public record ExamBuilderResponse(ExamSummaryResponse exam, List<BuilderQuestion> questions) {

    public record BuilderQuestion(long questionId, int position, BigDecimal score, boolean required,
            TeacherQuestion question) {
    }

    /** Phần của OpenAPI {@code QuestionTeacherView} mà {@code QuestionQueryService} của C cung cấp. */
    public record TeacherQuestion(long id, long courseId, String content, QuestionType questionType,
            Difficulty difficulty, String skillCode, String explanation, boolean active, List<TeacherOption> options) {

        public static TeacherQuestion from(QuestionView q) {
            return new TeacherQuestion(q.id(), q.courseId(), q.content(), q.type(), q.difficulty(), q.skillCode(),
                    q.explanation(), q.active(), q.options().stream()
                            .map(o -> new TeacherOption(o.id(), o.content(), o.correct(), o.position())).toList());
        }
    }

    public record TeacherOption(long id, String content, boolean correct, int position) {
    }
}
