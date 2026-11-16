package com.example.lms.course;

import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(@Size(max = 2000) String reason) {
}
