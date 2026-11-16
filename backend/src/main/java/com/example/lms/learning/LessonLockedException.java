package com.example.lms.learning;

import com.example.lms.course.LessonPrerequisiteRef;
import com.example.lms.shared.exception.ErrorCode;

import java.time.Instant;

public class LessonLockedException extends RuntimeException {

    private final ErrorCode code;
    private final Instant releaseAt;
    private final LessonPrerequisiteRef prerequisite;

    public LessonLockedException(
            ErrorCode code,
            Instant releaseAt,
            LessonPrerequisiteRef prerequisite) {
        super("Bài học hiện chưa được mở.");
        this.code = code;
        this.releaseAt = releaseAt;
        this.prerequisite = prerequisite;
    }

    public ErrorCode code() {
        return code;
    }

    public Instant releaseAt() {
        return releaseAt;
    }

    public LessonPrerequisiteRef prerequisite() {
        return prerequisite;
    }
}
