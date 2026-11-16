package com.example.lms.course;

import com.example.lms.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "course_teachers")
public class CourseTeacher {

    @EmbeddedId
    private CourseTeacherId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("courseId")
    @JoinColumn(name = "course_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("teacherId")
    @JoinColumn(name = "teacher_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User teacher;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", columnDefinition = "BIGINT UNSIGNED")
    private User assignedBy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();

    @Column(name = "removed_at")
    private Instant removedAt;

    protected CourseTeacher() {
    }

    public CourseTeacher(Course course, User teacher) {
        this.course = course;
        this.teacher = teacher;
        this.id = new CourseTeacherId(course.getId(), teacher.getId());
        this.primary = true;
        this.active = true;
    }

    public CourseTeacherId getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public User getTeacher() {
        return teacher;
    }

    public boolean isPrimary() {
        return primary;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void assign(boolean primary, User assignedBy) {
        this.primary = primary;
        this.active = true;
        this.assignedBy = assignedBy;
        this.removedAt = null;
    }

    public void remove() {
        this.active = false;
        this.primary = false;
        this.removedAt = Instant.now();
    }

}
