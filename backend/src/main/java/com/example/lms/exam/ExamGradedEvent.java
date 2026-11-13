package com.example.lms.exam;

import java.math.BigDecimal;

/**
 * Phát trong transaction chấm bài (interface M5). A nghe bằng {@code @EventListener} đồng bộ để cập nhật
 * LessonProgress ({@code PASS_EXAM}/{@code SUBMIT_EXAM}) và Course completion trong cùng transaction (ERD Mục 14).
 */
public record ExamGradedEvent(long attemptId, long studentId, long examId, long lessonId, boolean passed,
        BigDecimal score, SubmissionType submissionType) {
}
