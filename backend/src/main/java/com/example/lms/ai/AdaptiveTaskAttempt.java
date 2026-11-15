package com.example.lms.ai;

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

/** Lần làm nhiệm vụ của Student (ERD 17.11). Idempotent theo (studentId, clientEventId). */
@Entity
@Table(name = "adaptive_task_attempts")
public class AdaptiveTaskAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, updatable = false)
    private long taskId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private long studentId;

    @Column(name = "client_event_id", nullable = false, updatable = false, length = 64)
    private String clientEventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_json")
    private String answerJson;

    @Column(name = "score", precision = 8, scale = 4)
    private BigDecimal score;

    @Column(name = "max_score", precision = 8, scale = 4)
    private BigDecimal maxScore;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "hints_used", nullable = false)
    private int hintsUsed;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_method", nullable = false)
    private GradingMethod gradingMethod;

    @Column(name = "grading_confidence", precision = 6, scale = 5)
    private BigDecimal gradingConfidence;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Column(name = "interaction_id")
    private Long interactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AdaptiveTaskAttempt() {
    }

    /** Lượt nộp mới; {@code (task, student)} và {@code (student, clientEventId)} bất biến và duy nhất. */
    public static AdaptiveTaskAttempt create(long taskId, long studentId, String clientEventId, Instant now) {
        AdaptiveTaskAttempt a = new AdaptiveTaskAttempt();
        a.taskId = taskId;
        a.studentId = studentId;
        a.clientEventId = clientEventId;
        a.submittedAt = now;
        a.createdAt = now;
        return a;
    }

    public Long getId() {
        return id;
    }

    public long getTaskId() {
        return taskId;
    }

    public long getStudentId() {
        return studentId;
    }

    public String getClientEventId() {
        return clientEventId;
    }

    public String getAnswerJson() {
        return answerJson;
    }

    public void setAnswerJson(String answerJson) {
        this.answerJson = answerJson;
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

    public String getFeedbackText() {
        return feedbackText;
    }

    public void setFeedbackText(String feedbackText) {
        this.feedbackText = feedbackText;
    }

    public int getHintsUsed() {
        return hintsUsed;
    }

    public void setHintsUsed(int hintsUsed) {
        this.hintsUsed = hintsUsed;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public GradingMethod getGradingMethod() {
        return gradingMethod;
    }

    public void setGradingMethod(GradingMethod gradingMethod) {
        this.gradingMethod = gradingMethod;
    }

    public BigDecimal getGradingConfidence() {
        return gradingConfidence;
    }

    public void setGradingConfidence(BigDecimal gradingConfidence) {
        this.gradingConfidence = gradingConfidence;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(Instant gradedAt) {
        this.gradedAt = gradedAt;
    }

    public Long getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(Long interactionId) {
        this.interactionId = interactionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
