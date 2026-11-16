package com.example.lms.learning;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LessonHeartbeatRequest(@Min(0) int positionSeconds, @NotNull UUID clientEventId) {
}
