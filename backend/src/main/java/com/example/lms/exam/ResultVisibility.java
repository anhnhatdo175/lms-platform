package com.example.lms.exam;

/**
 * Được xem gì sau khi chấm (Spec 14.7). {@code AFTER_EXAM_CLOSED} (SHOULD): ẩn điểm và đáp án tới khi Exam
 * đóng, sau đó như {@code IMMEDIATE_WITH_CORRECT_ANSWERS}.
 */
public record ResultVisibility(boolean score, boolean answers) {

    public static ResultVisibility of(ResultDisplayMode mode, boolean examClosed) {
        return switch (mode) {
            case IMMEDIATE_SCORE_ONLY -> new ResultVisibility(true, false);
            case IMMEDIATE_WITH_CORRECT_ANSWERS -> new ResultVisibility(true, true);
            case AFTER_EXAM_CLOSED -> new ResultVisibility(examClosed, examClosed);
        };
    }
}
