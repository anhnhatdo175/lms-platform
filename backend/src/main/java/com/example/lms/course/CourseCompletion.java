package com.example.lms.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "course_completions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_completions_student_course",
                columnNames = {"student_id", "course_id"}))
public class CourseCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "student_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long studentId;

    @Column(name = "course_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_rule", nullable = false, length = 60)
    private CourseCompletionRule completionRule;

    @Column(name = "progress_percent_snapshot", nullable = false, precision = 5, scale = 2)
    private BigDecimal progressPercentSnapshot;

    @Column(name = "mandatory_lessons_snapshot", nullable = false)
    private int mandatoryLessonsSnapshot;

    @Column(name = "completed_lessons_snapshot", nullable = false)
    private int completedLessonsSnapshot;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "created_by", columnDefinition = "BIGINT UNSIGNED")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CourseCompletion() {
    }

    public CourseCompletion(
            long studentId,
            long courseId,
            CourseCompletionRule completionRule,
            BigDecimal progressPercentSnapshot,
            int mandatoryLessonsSnapshot,
            int completedLessonsSnapshot,
            Instant completedAt) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.completionRule = completionRule;
        this.progressPercentSnapshot = progressPercentSnapshot;
        this.mandatoryLessonsSnapshot = mandatoryLessonsSnapshot;
        this.completedLessonsSnapshot = completedLessonsSnapshot;
        this.completedAt = completedAt;
        this.createdAt = completedAt;
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

    public Instant getCompletedAt() {
        return completedAt;
    }
}
