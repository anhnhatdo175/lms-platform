package com.example.lms.ai.profile;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.example.lms.ai.session.ZpdPolicy;

/**
 * Công thức hồ sơ người học (thuần, không phụ thuộc Spring/DB).
 * <ul>
 * <li>EMA: {@code m' = (1-α)·m + α·outcome}, α = 0.3 (BR-AI-14). Bằng chứng đầu tiên bắt đầu từ mức trung tính 0.5
 * (BR-AI-13), không phải từ giá trị DB mặc định.</li>
 * <li>Phạt gợi ý: kết quả đúng bị nhân {@code max(0, 1 - λh·hintsUsed)}, λh = 0.3; kết quả sai giữ nguyên 0.</li>
 * </ul>
 */
public final class ProfileMath {

    public static final double ALPHA = 0.3;
    public static final double HINT_PENALTY = 0.3;
    public static final String MODEL_VERSION = "ema-0.3";
    public static final double STRONG_AT = 0.7;
    public static final double WEAK_BELOW = 0.4;

    private ProfileMath() {
    }

    /** Kết quả [0,1] của một lần làm bài, đã tính phạt gợi ý. */
    public static double outcome(BigDecimal score, BigDecimal maxScore, Boolean correct, int hintsUsed) {
        double base;
        if (score != null && maxScore != null && maxScore.signum() > 0) {
            base = score.doubleValue() / maxScore.doubleValue();
        } else {
            base = Boolean.TRUE.equals(correct) ? 1.0 : 0.0;
        }
        base = clamp(base);
        double factor = Math.max(0.0, 1.0 - HINT_PENALTY * Math.max(0, hintsUsed));
        return base * factor;
    }

    public static double nextMastery(double current, int evidenceCount, double outcome) {
        double start = evidenceCount <= 0 ? ZpdPolicy.COLD_START_MASTERY : current;
        return clamp((1 - ALPHA) * start + ALPHA * outcome);
    }

    public static BigDecimal scale(double value) {
        return BigDecimal.valueOf(clamp(value)).setScale(5, RoundingMode.HALF_UP);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
