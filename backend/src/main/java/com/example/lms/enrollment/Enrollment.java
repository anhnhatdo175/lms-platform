package com.example.lms.enrollment;

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

/** Quyền học một Course. Tạo ra là ACTIVE (Spec 12.3); lịch sử gia hạn giữ thành nhiều bản ghi. */
@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private Long courseId;

    @Column(name = "order_item_id", updatable = false)
    private Long orderItemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EnrollmentSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    @Column(name = "starts_at", nullable = false, updatable = false)
    private Instant startsAt;

    @Column(name = "expires_at", updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private Long revokedBy;

    @Column(name = "revoked_reason", length = 500)
    private String revokedReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Enrollment() {
    }

    public static Enrollment grant(long studentId, long courseId, Long orderItemId, EnrollmentSource source,
            Instant startsAt, Instant expiresAt, Instant now) {
        if (expiresAt != null && !expiresAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("expiresAt must be after startsAt");
        }
        Enrollment enrollment = new Enrollment();
        enrollment.studentId = studentId;
        enrollment.courseId = courseId;
        enrollment.orderItemId = orderItemId;
        enrollment.source = source;
        enrollment.status = EnrollmentStatus.ACTIVE;
        enrollment.startsAt = startsAt;
        enrollment.expiresAt = expiresAt;
        enrollment.createdAt = now;
        enrollment.updatedAt = now;
        return enrollment;
    }

    /** Cho học khi ACTIVE và {@code startsAt ≤ at < expiresAt} (Spec 12.3); không phụ thuộc job hết hạn đã chạy hay chưa. */
    public boolean isEffectiveAt(Instant at) {
        return status == EnrollmentStatus.ACTIVE && !at.isBefore(startsAt)
                && (expiresAt == null || at.isBefore(expiresAt));
    }

    /** Còn ACTIVE trong database nhưng đã qua {@code expiresAt}: job chưa kịp chạy. */
    public boolean isLapsedAt(Instant at) {
        return status == EnrollmentStatus.ACTIVE && expiresAt != null && !at.isBefore(expiresAt);
    }

    public void expire(Instant now) {
        transitionTo(EnrollmentStatus.EXPIRED);
        updatedAt = now;
    }

    public void revoke(long adminId, String reason, Instant now) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Revoking an enrollment requires a reason");
        }
        transitionTo(EnrollmentStatus.REVOKED);
        revokedAt = now;
        revokedBy = adminId;
        revokedReason = reason;
        updatedAt = now;
    }

    private void transitionTo(EnrollmentStatus target) {
        if (!status.canTransitionTo(target)) {
            throw BusinessException.stateConflict(
                    "Enrollment " + id + " cannot move from " + status + " to " + target);
        }
        status = target;
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

    public Long getOrderItemId() {
        return orderItemId;
    }

    public EnrollmentSource getSource() {
        return source;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Long getRevokedBy() {
        return revokedBy;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
