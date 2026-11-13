package com.example.lms.exam;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Câu trả lời cho một câu snapshot; autosave ghi đè, chấm điền {@code correct} và {@code scoreAwarded}. */
@Entity
@Table(name = "attempt_answers")
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false, updatable = false)
    private Long attemptId;

    @Column(name = "attempt_question_id", nullable = false, updatable = false)
    private Long attemptQuestionId;

    private Boolean correct;

    @Column(name = "score_awarded", precision = 7, scale = 2)
    private BigDecimal scoreAwarded;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AttemptAnswer() {
    }

    public AttemptAnswer(long attemptId, long attemptQuestionId, Instant now) {
        this.attemptId = attemptId;
        this.attemptQuestionId = attemptQuestionId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markAnswered(Instant now) {
        this.answeredAt = now;
        this.updatedAt = now;
    }

    public void grade(boolean correct, BigDecimal scoreAwarded, Instant now) {
        this.correct = correct;
        this.scoreAwarded = scoreAwarded;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public Long getAttemptQuestionId() {
        return attemptQuestionId;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public BigDecimal getScoreAwarded() {
        return scoreAwarded;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
