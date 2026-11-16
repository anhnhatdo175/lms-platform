package com.example.lms.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AssignTeachersRequest(
        @NotEmpty List<@Valid TeacherAssignmentRequest> teachers) {
}
