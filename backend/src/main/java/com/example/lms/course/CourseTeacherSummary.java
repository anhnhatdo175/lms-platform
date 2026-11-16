package com.example.lms.course;

import java.time.Instant;

public record CourseTeacherSummary(
        Long teacherId,
        String fullName,
        String email,
        boolean primary,
        boolean active,
        Instant assignedAt) {

    static CourseTeacherSummary from(CourseTeacher assignment) {
        return new CourseTeacherSummary(
                assignment.getTeacher().getId(),
                assignment.getTeacher().getFullName(),
                assignment.getTeacher().getEmail(),
                assignment.isPrimary(),
                assignment.isActive(),
                assignment.getAssignedAt());
    }
}
