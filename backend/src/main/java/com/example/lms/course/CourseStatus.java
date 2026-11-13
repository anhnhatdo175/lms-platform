package com.example.lms.course;

public enum CourseStatus {
    DRAFT,
    PENDING_REVIEW,
    REJECTED,
    PUBLISHED,
    HIDDEN,
    ARCHIVED;

    public boolean allowsContentChanges() {
        return this == DRAFT || this == PUBLISHED || this == HIDDEN;
    }
}
