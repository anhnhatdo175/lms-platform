package com.example.lms.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lesson_progress_student_course_lesson",
                columnNames = {"student_id", "course_id", "lesson_id"}))
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "student_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long studentId;

    @Column(name = "course_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long courseId;

    @Column(name = "lesson_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long lessonId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LessonProgressStatus status;

    @Column(name = "watched_seconds", nullable = false, columnDefinition = "INT UNSIGNED")
    private int watchedSeconds;

    @Column(name = "progress_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal progressPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_source", length = 30)
    private LessonCompletionSource completionSource;

    @Column(name = "first_accessed_at")
    private Instant firstAccessedAt;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_heartbeat_event_id", length = 64)
    private String lastHeartbeatEventId;

    @Column(name = "last_completion_event_id", length = 64)
    private String lastCompletionEventId;

    @Column(name = "overridden_by", columnDefinition = "BIGINT UNSIGNED")
    private Long overriddenBy;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonProgress() {
    }

    public LessonProgress(long studentId, long courseId, long lessonId, Instant now) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.status = LessonProgressStatus.NOT_STARTED;
        this.watchedSeconds = 0;
        this.progressPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    void setUpdatedAt() {
        updatedAt = Instant.now();
    }

    public boolean isDuplicateHeartbeat(String eventId) {
        return eventId != null && eventId.equals(lastHeartbeatEventId);
    }

    public boolean isDuplicateCompletion(String eventId) {
        return eventId != null && eventId.equals(lastCompletionEventId);
    }

    public void touch(Instant now) {
        if (firstAccessedAt == null) firstAccessedAt = now;
        lastAccessedAt = now;
        if (status == LessonProgressStatus.NOT_STARTED) status = LessonProgressStatus.IN_PROGRESS;
        updatedAt = now;
    }

    public void recordHeartbeat(String eventId, int positionSeconds, Integer durationSeconds, Instant now) {
        touch(now);
        lastHeartbeatEventId = eventId;
        watchedSeconds = Math.max(watchedSeconds, positionSeconds);
        progressPercent = durationSeconds == null || durationSeconds < 1
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(watchedSeconds)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(durationSeconds), 2, RoundingMode.HALF_UP)
                        .min(BigDecimal.valueOf(100));
    }

    public boolean complete(LessonCompletionSource source, Instant now) {
        return complete(source, now, null);
    }

    public boolean complete(LessonCompletionSource source, Instant now, String eventId) {
        if (status == LessonProgressStatus.COMPLETED) return false;
        touch(now);
        status = LessonProgressStatus.COMPLETED;
        completionSource = source;
        completedAt = now;
        lastCompletionEventId = eventId;
        return true;
    }

    public void reset(Instant now) {
        status = LessonProgressStatus.NOT_STARTED;
        watchedSeconds = 0;
        progressPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        completionSource = null;
        completedAt = null;
        lastHeartbeatEventId = null;
        lastCompletionEventId = null;
        overriddenBy = null;
        overrideReason = null;
        updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public LessonProgressStatus getStatus() {
        return status;
    }

    public int getWatchedSeconds() {
        return watchedSeconds;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public LessonCompletionSource getCompletionSource() {
        return completionSource;
    }

    public Instant getFirstAccessedAt() {
        return firstAccessedAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getLastHeartbeatEventId() {
        return lastHeartbeatEventId;
    }

    public Long getOverriddenBy() {
        return overriddenBy;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
