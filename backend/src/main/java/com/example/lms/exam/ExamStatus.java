package com.example.lms.exam;

public enum ExamStatus {
    DRAFT,
    PUBLISHED,
    CLOSED;

    public boolean canTransitionTo(ExamStatus target) {
        return (this == DRAFT && target == PUBLISHED) || (this == PUBLISHED && target == CLOSED);
    }
}
