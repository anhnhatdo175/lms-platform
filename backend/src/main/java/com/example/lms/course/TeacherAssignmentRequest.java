package com.example.lms.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TeacherAssignmentRequest(
        @NotNull @Min(1) Long teacherId,
        boolean primary) {
}
