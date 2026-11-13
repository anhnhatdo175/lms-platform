package com.example.lms.exam;

import java.time.Duration;
import java.time.Instant;

/** Hạn của Attempt: sớm hơn giữa (bắt đầu + thời lượng) và lúc Exam đóng (Doc phần B, Mục 13 ý 12). */
public final class AttemptWindow {

    private AttemptWindow() {
    }

    public static Instant expiresAt(Instant startedAt, int durationMinutes, Instant examEndAt) {
        Instant byDuration = startedAt.plus(Duration.ofMinutes(durationMinutes));
        return examEndAt != null && examEndAt.isBefore(byDuration) ? examEndAt : byDuration;
    }
}
