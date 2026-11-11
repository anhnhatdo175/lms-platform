package com.example.lms.exam;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Interface M4 cho A: prerequisite {@code PASS_EXAM} và rule completion có bài thi bắt buộc. */
@Service
@Transactional(readOnly = true)
public class ExamResultQueryService {

    private final ExamAttemptRepository attempts;

    public ExamResultQueryService(ExamAttemptRepository attempts) {
        this.attempts = attempts;
    }

    /** Có ít nhất một Attempt đã chấm và đạt; Attempt hết giờ cũng tính nếu điểm đạt. */
    public boolean hasPassed(long studentId, long examId) {
        return attempts.existsByExamIdAndStudentIdAndStatusAndPassedTrue(examId, studentId, AttemptStatus.GRADED);
    }
}
