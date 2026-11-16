package com.example.lms.course;

import jakarta.validation.constraints.Size;

public record ReviewSubmissionRequest(@Size(max = 2000) String note) {
}
