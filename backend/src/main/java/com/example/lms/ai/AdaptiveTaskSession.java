package com.example.lms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Một lần Student yêu cầu bộ nhiệm vụ cá nhân hóa (ERD 17.9). attemptCount là fencing token. */
@Entity
@Table(name = "adaptive_task_sessions")
public class AdaptiveTaskSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private long studentId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private long courseId;

    @Column(name = "lesson_id", updatable = false)
    private Long lessonId;

    @Column(name = "config_id")
    private Long configId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "constraints_json")
    private String constraintsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requested_topics_json")
    private String requestedTopicsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requested_task_types_json")
    private String requestedTaskTypesJson;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "task_count", nullable = false)
    private int taskCount;

    @Column(name = "client_request_id", updatable = false, length = 64)
    private String clientRequestId;

    @Column(name = "pipeline_version", length = 100)
    private String pipelineVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "retrieval_trace_json")
    private String retrievalTraceJson;

    @Column(name = "profile_version")
    private Integer profileVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_code")
    private SessionErrorCode errorCode;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "auto_retry_count", nullable = false)
    private int autoRetryCount;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "manual_retry_count", nullable = false)
    private int manualRetryCount;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "worker_id", length = 100)
    private String workerId;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public AdaptiveTaskSession() {
    }

    public Long getId() {
        return id;
    }

    public long getStudentId() {
        return studentId;
    }

    public long getCourseId() {
        return courseId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public String getConstraintsJson() {
        return constraintsJson;
    }

    public void setConstraintsJson(String constraintsJson) {
        this.constraintsJson = constraintsJson;
    }

    public String getRequestedTopicsJson() {
        return requestedTopicsJson;
    }

    public void setRequestedTopicsJson(String requestedTopicsJson) {
        this.requestedTopicsJson = requestedTopicsJson;
    }

    public String getRequestedTaskTypesJson() {
        return requestedTaskTypesJson;
    }

    public void setRequestedTaskTypesJson(String requestedTaskTypesJson) {
        this.requestedTaskTypesJson = requestedTaskTypesJson;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public String getPipelineVersion() {
        return pipelineVersion;
    }

    public void setPipelineVersion(String pipelineVersion) {
        this.pipelineVersion = pipelineVersion;
    }

    public String getRetrievalTraceJson() {
        return retrievalTraceJson;
    }

    public void setRetrievalTraceJson(String retrievalTraceJson) {
        this.retrievalTraceJson = retrievalTraceJson;
    }

    public Integer getProfileVersion() {
        return profileVersion;
    }

    public void setProfileVersion(Integer profileVersion) {
        this.profileVersion = profileVersion;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public SessionErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(SessionErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getAutoRetryCount() {
        return autoRetryCount;
    }

    public void setAutoRetryCount(int autoRetryCount) {
        this.autoRetryCount = autoRetryCount;
    }

    public int getManualRetryCount() {
        return manualRetryCount;
    }

    public void setManualRetryCount(int manualRetryCount) {
        this.manualRetryCount = manualRetryCount;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

}
