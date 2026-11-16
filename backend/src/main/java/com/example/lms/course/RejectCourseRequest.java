package com.example.lms.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCourseRequest(@NotBlank @Size(max = 2000) String reason) {
}
