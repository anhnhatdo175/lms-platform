package com.example.lms.exam;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_id", nullable = false, updatable = false)
    private Long lessonId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "passing_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal passingScore;

    @Column(name = "maximum_attempts")
    private Integer maximumAttempts;

    @Column(name = "shuffle_questions", nullable = false)
    private boolean shuffleQuestions;

    @Column(name = "shuffle_options", nullable = false)
    private boolean shuffleOptions;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_display_mode", nullable = false)
    private ResultDisplayMode resultDisplayMode;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamStatus status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Exam() {
    }

    public Exam(long lessonId, String title, String description, int durationMinutes, BigDecimal passingScore,
            Integer maximumAttempts, boolean shuffleQuestions, boolean shuffleOptions,
            ResultDisplayMode resultDisplayMode, Instant startAt, Instant endAt, long createdBy, Instant now) {
        this.lessonId = lessonId;
        this.createdBy = createdBy;
        this.status = ExamStatus.DRAFT;
        this.createdAt = now;
        assignSettings(title, description, durationMinutes, passingScore, maximumAttempts, shuffleQuestions,
                shuffleOptions, resultDisplayMode == null ? ResultDisplayMode.IMMEDIATE_SCORE_ONLY : resultDisplayMode,
                startAt, endAt, now);
    }

    /**
     * Cài đặt chung. Attempt đã bắt đầu không bị ảnh hưởng vì đã snapshot {@code expiresAt},
     * {@code maximum_score} và {@code passing_score}.
     */
    public void applySettings(String title, String description, int durationMinutes, BigDecimal passingScore,
            Integer maximumAttempts, boolean shuffleQuestions, boolean shuffleOptions,
            ResultDisplayMode resultDisplayMode, Instant startAt, Instant endAt, Instant now) {
        assignSettings(title, description, durationMinutes, passingScore, maximumAttempts, shuffleQuestions,
                shuffleOptions, resultDisplayMode, startAt, endAt, now);
    }

    private void assignSettings(String title, String description, int durationMinutes, BigDecimal passingScore,
            Integer maximumAttempts, boolean shuffleQuestions, boolean shuffleOptions,
            ResultDisplayMode resultDisplayMode, Instant startAt, Instant endAt, Instant now) {
        if (durationMinutes < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "durationMinutes must be at least 1");
        }
        if (passingScore.signum() < 0 || passingScore.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "passingScore must be between 0 and 100");
        }
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "endAt must be after startAt");
        }
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.passingScore = passingScore;
        this.maximumAttempts = maximumAttempts;
        this.shuffleQuestions = shuffleQuestions;
        this.shuffleOptions = shuffleOptions;
        this.resultDisplayMode = resultDisplayMode;
        this.startAt = startAt;
        this.endAt = endAt;
        this.updatedAt = now;
    }

    public void publish(Instant now) {
        transitionTo(ExamStatus.PUBLISHED);
        updatedAt = now;
    }

    public void close(Instant now) {
        transitionTo(ExamStatus.CLOSED);
        updatedAt = now;
    }

    /** Nhận Attempt mới tại thời điểm {@code at}: PUBLISHED và trong khoảng mở (nếu có). */
    public boolean isOpenAt(Instant at) {
        return status == ExamStatus.PUBLISHED && (startAt == null || !at.isBefore(startAt))
                && (endAt == null || at.isBefore(endAt));
    }

    private void transitionTo(ExamStatus target) {
        if (!status.canTransitionTo(target)) {
            throw BusinessException.stateConflict("Exam " + id + " cannot move from " + status + " to " + target);
        }
        status = target;
    }

    public Long getId() {
        return id;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public BigDecimal getPassingScore() {
        return passingScore;
    }

    public Integer getMaximumAttempts() {
        return maximumAttempts;
    }

    public boolean isShuffleQuestions() {
        return shuffleQuestions;
    }

    public boolean isShuffleOptions() {
        return shuffleOptions;
    }

    public ResultDisplayMode getResultDisplayMode() {
        return resultDisplayMode;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
