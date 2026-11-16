package com.example.lms.course;

import jakarta.validation.constraints.Size;

public record HideCourseRequest(@Size(max = 2000) String reason) {
}
