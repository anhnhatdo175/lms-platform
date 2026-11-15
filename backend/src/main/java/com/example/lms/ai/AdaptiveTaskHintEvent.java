package com.example.lms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Một lần cấp gợi ý Socratic (ERD 17.12, SHOULD). Bảng riêng, không dùng learning_interactions. */
@Entity
@Table(name = "adaptive_task_hint_events")
public class AdaptiveTaskHintEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, updatable = false)
    private long taskId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private long studentId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 64)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "hint_index", nullable = false, updatable = false)
    private int hintIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AdaptiveTaskHintEvent() {
    }

    public static AdaptiveTaskHintEvent create(long taskId, long studentId, String idempotencyKey, int hintIndex,
            Instant now) {
        AdaptiveTaskHintEvent e = new AdaptiveTaskHintEvent();
        e.taskId = taskId;
        e.studentId = studentId;
        e.idempotencyKey = idempotencyKey;
        e.hintIndex = hintIndex;
        e.createdAt = now;
        return e;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public int getHintIndex() {
        return hintIndex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
