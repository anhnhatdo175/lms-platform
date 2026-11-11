package com.example.lms.exam;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.example.lms.contract.question.Difficulty;
import com.example.lms.contract.question.QuestionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Snapshot một câu hỏi lúc bắt đầu Attempt (Spec 14.4). Chấm, autosave và hiển thị chỉ đọc bảng này,
 * không đọc lại Question gốc, nên Teacher sửa Question không đổi Attempt đang diễn ra (AC-EXAM-02).
 *
 * <p>{@code optionSnapshotJson} chứa cờ {@code correct}: không bao giờ serialize thẳng ra response cho Student.
 */
@Entity
@Table(name = "attempt_questions")
public class AttemptQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false, updatable = false)
    private Long attemptId;

    @Column(name = "source_question_id", nullable = false, updatable = false)
    private Long sourceQuestionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, updatable = false)
    private QuestionType questionType;

    @Column(name = "content_snapshot", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String contentSnapshot;

    @Column(name = "explanation_snapshot", updatable = false, columnDefinition = "TEXT")
    private String explanationSnapshot;

    @Column(name = "skill_code_snapshot", updatable = false, length = 150)
    private String skillCodeSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_snapshot", nullable = false, updatable = false)
    private Difficulty difficultySnapshot;

    @Column(nullable = false, updatable = false)
    private int position;

    @Column(name = "maximum_score", nullable = false, updatable = false, precision = 7, scale = 2)
    private BigDecimal maximumScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "option_snapshot_json", nullable = false, updatable = false)
    private String optionSnapshotJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AttemptQuestion() {
    }

    public AttemptQuestion(long attemptId, long sourceQuestionId, QuestionType questionType, String contentSnapshot,
            String explanationSnapshot, String skillCodeSnapshot, Difficulty difficultySnapshot, int position,
            BigDecimal maximumScore, String optionSnapshotJson, Instant now) {
        this.attemptId = attemptId;
        this.sourceQuestionId = sourceQuestionId;
        this.questionType = questionType;
        this.contentSnapshot = contentSnapshot;
        this.explanationSnapshot = explanationSnapshot;
        this.skillCodeSnapshot = skillCodeSnapshot;
        this.difficultySnapshot = difficultySnapshot;
        this.position = position;
        this.maximumScore = maximumScore;
        this.optionSnapshotJson = optionSnapshotJson;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public Long getSourceQuestionId() {
        return sourceQuestionId;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public String getContentSnapshot() {
        return contentSnapshot;
    }

    public String getExplanationSnapshot() {
        return explanationSnapshot;
    }

    public String getSkillCodeSnapshot() {
        return skillCodeSnapshot;
    }

    public Difficulty getDifficultySnapshot() {
        return difficultySnapshot;
    }

    public int getPosition() {
        return position;
    }

    public BigDecimal getMaximumScore() {
        return maximumScore;
    }

    public String getOptionSnapshotJson() {
        return optionSnapshotJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
