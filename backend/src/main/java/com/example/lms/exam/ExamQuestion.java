package com.example.lms.exam;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Câu hỏi trong Exam. Question là của C; B chỉ giữ {@code question_id}, vị trí và điểm. */
@Entity
@Table(name = "exam_questions")
@IdClass(ExamQuestion.Key.class)
public class ExamQuestion {

    @Id
    @Column(name = "exam_id")
    private Long examId;

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal score;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExamQuestion() {
    }

    public ExamQuestion(long examId, long questionId, int position, BigDecimal score, boolean required,
            Instant now) {
        if (score.signum() < 0) {
            throw new IllegalArgumentException("score must not be negative");
        }
        this.examId = examId;
        this.questionId = questionId;
        this.position = position;
        this.score = score;
        this.required = required;
        this.createdAt = now;
    }

    public Long getExamId() {
        return examId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public int getPosition() {
        return position;
    }

    public BigDecimal getScore() {
        return score;
    }

    public boolean isRequired() {
        return required;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class Key implements Serializable {
        private Long examId;
        private Long questionId;

        public Key() {
        }

        public Key(Long examId, Long questionId) {
            this.examId = examId;
            this.questionId = questionId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && Objects.equals(examId, key.examId)
                    && Objects.equals(questionId, key.questionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(examId, questionId);
        }
    }
}
