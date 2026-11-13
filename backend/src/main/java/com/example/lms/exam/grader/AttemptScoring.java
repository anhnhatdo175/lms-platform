package com.example.lms.exam.grader;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Công thức điểm của Spec 14.6: {@code score = tổng điểm nhận / tổng điểm tối đa × 100}, đạt khi ≥ điểm đạt. */
public final class AttemptScoring {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private AttemptScoring() {
    }

    /** Làm tròn HALF_UP 2 chữ số, khớp {@code exam_attempts.score DECIMAL(7,2)}. */
    public static BigDecimal percent(BigDecimal awarded, BigDecimal maximum) {
        if (maximum.signum() <= 0) {
            // Publish đã chặn Exam có tổng điểm 0 (EXAM_NOT_READY); tới đây là lỗi dữ liệu
            throw new IllegalStateException("maximum score must be positive");
        }
        return awarded.multiply(HUNDRED).divide(maximum, 2, RoundingMode.HALF_UP);
    }

    public static boolean passed(BigDecimal score, BigDecimal passingScore) {
        return score.compareTo(passingScore) >= 0;
    }
}
