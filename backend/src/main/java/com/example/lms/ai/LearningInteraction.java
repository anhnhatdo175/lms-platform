package com.example.lms.ai;

import com.example.lms.contract.question.Difficulty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Nhật ký tín hiệu chuẩn hóa, nguồn của ma trận R (ERD 17.8). event_key bảo đảm idempotent (BR-AI-08). */
@Entity
@Table(name = "learning_interactions")
public class LearningInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private long studentId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "skill_code", length = 150)
    private String skillCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, updatable = false)
    private InteractionItemType itemType;

    @Column(name = "item_id", nullable = false, updatable = false)
    private long itemId;

    @Column(name = "source_attempt_id")
    private Long sourceAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level")
    private Difficulty difficultyLevel;

    @Column(name = "score", precision = 8, scale = 4)
    private BigDecimal score;

    @Column(name = "max_score", precision = 8, scale = 4)
    private BigDecimal maxScore;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "hints_used", nullable = false)
    private int hintsUsed;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "event_key", nullable = false, updatable = false, length = 150)
    private String eventKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json")
    private String metadataJson;

    public LearningInteraction() {
    }

    public Long getId() {
        return id;
    }

    public long getStudentId() {
        return studentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public String getSkillCode() {
        return skillCode;
    }

    public void setSkillCode(String skillCode) {
        this.skillCode = skillCode;
    }

    public InteractionItemType getItemType() {
        return itemType;
    }

    public long getItemId() {
        return itemId;
    }

    public Long getSourceAttemptId() {
        return sourceAttemptId;
    }

    public void setSourceAttemptId(Long sourceAttemptId) {
        this.sourceAttemptId = sourceAttemptId;
    }

    public Difficulty getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(Difficulty difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public int getHintsUsed() {
        return hintsUsed;
    }

    public void setHintsUsed(int hintsUsed) {
        this.hintsUsed = hintsUsed;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

}
