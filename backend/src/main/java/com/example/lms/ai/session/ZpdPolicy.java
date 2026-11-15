package com.example.lms.ai.session;

import com.example.lms.contract.question.Difficulty;

/**
 * Chọn độ khó mục tiêu khi sinh nhiệm vụ: {@code target = M + δ}, làm tròn về mức EASY/MEDIUM/HARD
 * (0.25/0.5/0.75) rồi kẹp trong [min, max] do Teacher cấu hình (Spec 20A, BR-AI-03, BR-AI-13).
 * Score_ZPD từng khúc của cô dùng để xếp hạng ứng viên ở Recommender, không dùng ở đây.
 */
public final class ZpdPolicy {

    public static final double DELTA = 0.10;
    public static final double COLD_START_MASTERY = 0.5;
    public static final int COLD_START_MIN_SIGNALS = 3;

    private ZpdPolicy() {
    }

    public static boolean isColdStart(int evidenceCount) {
        return evidenceCount < COLD_START_MIN_SIGNALS;
    }

    /** Dưới 3 tín hiệu thì coi mức thành thạo là trung tính 0.5 (BR-AI-13). */
    public static double effectiveMastery(double mastery, int evidenceCount) {
        return isColdStart(evidenceCount) ? COLD_START_MASTERY : clamp01(mastery);
    }

    public static double numeric(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 0.25;
            case MEDIUM -> 0.5;
            case HARD -> 0.75;
        };
    }

    public static Difficulty target(double mastery, int evidenceCount, Difficulty min, Difficulty max) {
        double raw = effectiveMastery(mastery, evidenceCount) + DELTA;
        Difficulty best = Difficulty.EASY;
        double bestGap = Double.MAX_VALUE;
        for (Difficulty d : Difficulty.values()) {
            double gap = Math.abs(numeric(d) - raw);
            if (gap < bestGap - 1e-9) {
                best = d;
                bestGap = gap;
            }
        }
        if (best.ordinal() < min.ordinal()) {
            return min;
        }
        if (best.ordinal() > max.ordinal()) {
            return max;
        }
        return best;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
