package com.example.lms.ai.session;

/** Ảnh chụp bất biến của phiên lúc worker nhận việc; {@code attemptCount} là fencing token của lần nhận này. */
public record SessionSnapshot(long id, long studentId, long courseId, Long lessonId, Long configId, int taskCount,
        String constraintsJson, String requestedTopicsJson, String requestedTaskTypesJson, int attemptCount,
        int autoRetryCount) {
}
