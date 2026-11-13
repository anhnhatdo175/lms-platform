package com.example.lms.exam;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.lms.shared.exception.BusinessException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Một lượt làm bài. {@code expiresAt} do server tính khi bắt đầu và là nguồn sự thật duy nhất, không có
 * ngưỡng nới (DEC-010).
 *
 * <p>{@code version} là token của API autosave (ERD "Autosave lock"): tăng mỗi lần lưu đáp án hoặc chấm, luôn
 * dưới khóa {@code FOR UPDATE}. Không dùng {@code @Version} của JPA vì autosave chỉ ghi bảng con, dòng Attempt
 * không đổi nên JPA sẽ không tăng version.
 */
@Entity
@Table(name = "exam_attempts")
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false, updatable = false)
    private Long examId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "enrollment_id", nullable = false, updatable = false)
    private Long enrollmentId;

    @Column(name = "attempt_number", nullable = false, updatable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type")
    private SubmissionType submissionType;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Column(precision = 7, scale = 2)
    private BigDecimal score;

    @Column(name = "maximum_score", nullable = false, updatable = false, precision = 7, scale = 2)
    private BigDecimal maximumScore;

    @Column(name = "passing_score", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal passingScore;

    private Boolean passed;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExamAttempt() {
    }

    public static ExamAttempt start(long examId, long studentId, long enrollmentId, int attemptNumber,
            Instant startedAt, Instant expiresAt, BigDecimal maximumScore, BigDecimal passingScore) {
        if (!expiresAt.isAfter(startedAt)) {
            throw new IllegalArgumentException("expiresAt must be after startedAt");
        }
        ExamAttempt attempt = new ExamAttempt();
        attempt.examId = examId;
        attempt.studentId = studentId;
        attempt.enrollmentId = enrollmentId;
        attempt.attemptNumber = attemptNumber;
        attempt.status = AttemptStatus.IN_PROGRESS;
        attempt.startedAt = startedAt;
        attempt.expiresAt = expiresAt;
        attempt.maximumScore = maximumScore;
        attempt.passingScore = passingScore;
        attempt.createdAt = startedAt;
        return attempt;
    }

    /** Hết hạn khi {@code now ≥ expiresAt}; đúng mốc {@code expiresAt} đã là quá hạn. */
    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isInProgress() {
        return status == AttemptStatus.IN_PROGRESS;
    }

    /**
     * Chốt và ghi điểm. Với AUTO_EXPIRED, {@code submittedAt} là {@code expiresAt}: bài được tính nộp đúng lúc
     * hết giờ, dù request hay job đến muộn hơn.
     */
    public void grade(SubmissionType type, BigDecimal score, boolean passed, Instant now) {
        if (!status.canTransitionTo(AttemptStatus.GRADED)) {
            throw BusinessException.stateConflict("Attempt " + id + " cannot move from " + status + " to GRADED");
        }
        this.status = AttemptStatus.GRADED;
        this.submissionType = type;
        this.submittedAt = type == SubmissionType.AUTO_EXPIRED ? expiresAt : now;
        this.gradedAt = now;
        this.score = score;
        this.passed = passed;
        this.version++;
    }

    /** Gọi dưới khóa Attempt mỗi lần lưu đáp án; client gửi version cũ sẽ bị từ chối. */
    public void bumpVersion() {
        version++;
    }

    public Long getId() {
        return id;
    }

    public Long getExamId() {
        return examId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public SubmissionType getSubmissionType() {
        return submissionType;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getGradedAt() {
        return gradedAt;
    }

    public BigDecimal getScore() {
        return score;
    }

    public BigDecimal getMaximumScore() {
        return maximumScore;
    }

    public BigDecimal getPassingScore() {
        return passingScore;
    }

    public Boolean getPassed() {
        return passed;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
