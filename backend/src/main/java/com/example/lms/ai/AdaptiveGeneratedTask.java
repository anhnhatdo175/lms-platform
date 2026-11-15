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

/** Nhiệm vụ trong một phiên (ERD 17.10). answerKeyJson tuyệt đối không trả cho Student trước khi nộp (AC-AI-12). */
@Entity
@Table(name = "adaptive_generated_tasks")
public class AdaptiveGeneratedTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private long sessionId;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "position", nullable = false, updatable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, updatable = false)
    private TaskOrigin origin;

    @Column(name = "source_question_id", updatable = false)
    private Long sourceQuestionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private AdaptiveTaskType taskType;

    @Column(name = "skill_code", nullable = false, length = 150)
    private String skillCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false)
    private Difficulty difficultyLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", nullable = false)
    private String contentJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_key_json", nullable = false)
    private String answerKeyJson;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "personalization_reason", nullable = false, columnDefinition = "TEXT")
    private String personalizationReason;

    @Column(name = "fit_score", precision = 6, scale = 5)
    private BigDecimal fitScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hint_steps_json")
    private String hintStepsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_chunk_ids_json")
    private String evidenceChunkIdsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false)
    private ValidationStatus validationStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors_json")
    private String validationErrorsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "teacher_review_status", nullable = false)
    private TeacherReviewStatus teacherReviewStatus;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "promoted_question_id")
    private Long promotedQuestionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AdaptiveGeneratedTask() {
    }

    public Long getId() {
        return id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public int getPosition() {
        return position;
    }

    public TaskOrigin getOrigin() {
        return origin;
    }

    public Long getSourceQuestionId() {
        return sourceQuestionId;
    }

    public AdaptiveTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(AdaptiveTaskType taskType) {
        this.taskType = taskType;
    }

    public String getSkillCode() {
        return skillCode;
    }

    public void setSkillCode(String skillCode) {
        this.skillCode = skillCode;
    }

    public Difficulty getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(Difficulty difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public String getAnswerKeyJson() {
        return answerKeyJson;
    }

    public void setAnswerKeyJson(String answerKeyJson) {
        this.answerKeyJson = answerKeyJson;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getPersonalizationReason() {
        return personalizationReason;
    }

    public void setPersonalizationReason(String personalizationReason) {
        this.personalizationReason = personalizationReason;
    }

    public BigDecimal getFitScore() {
        return fitScore;
    }

    public void setFitScore(BigDecimal fitScore) {
        this.fitScore = fitScore;
    }

    public String getHintStepsJson() {
        return hintStepsJson;
    }

    public void setHintStepsJson(String hintStepsJson) {
        this.hintStepsJson = hintStepsJson;
    }

    public String getEvidenceChunkIdsJson() {
        return evidenceChunkIdsJson;
    }

    public void setEvidenceChunkIdsJson(String evidenceChunkIdsJson) {
        this.evidenceChunkIdsJson = evidenceChunkIdsJson;
    }

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getValidationErrorsJson() {
        return validationErrorsJson;
    }

    public void setValidationErrorsJson(String validationErrorsJson) {
        this.validationErrorsJson = validationErrorsJson;
    }

    public TeacherReviewStatus getTeacherReviewStatus() {
        return teacherReviewStatus;
    }

    public void setTeacherReviewStatus(TeacherReviewStatus teacherReviewStatus) {
        this.teacherReviewStatus = teacherReviewStatus;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public Long getPromotedQuestionId() {
        return promotedQuestionId;
    }

    public void setPromotedQuestionId(Long promotedQuestionId) {
        this.promotedQuestionId = promotedQuestionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
