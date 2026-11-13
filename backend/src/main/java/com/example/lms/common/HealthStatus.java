package com.example.lms.common;

import java.time.Instant;
import java.util.Map;

public record HealthStatus(
        String status,
        Instant timestamp,
        Map<String, String> dependencies) {
}
