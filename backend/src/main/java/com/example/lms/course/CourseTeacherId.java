package com.example.lms.course;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CourseTeacherId implements Serializable {

    @Column(name = "course_id", columnDefinition = "BIGINT UNSIGNED")
    private Long courseId;

    @Column(name = "teacher_id", columnDefinition = "BIGINT UNSIGNED")
    private Long teacherId;

    protected CourseTeacherId() {
    }

    public CourseTeacherId(Long courseId, Long teacherId) {
        this.courseId = courseId;
        this.teacherId = teacherId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CourseTeacherId that)) return false;
        return Objects.equals(courseId, that.courseId) && Objects.equals(teacherId, that.teacherId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseId, teacherId);
    }
}
