package com.example.lms.exam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.example.lms.exam.dto.ExamStatisticsResponse.ScoreBucket;

/** Phép tính thuần cho thống kê Exam, tách khỏi repository để kiểm thử. */
public final class ExamStatistics {

    /** Mười khoảng điểm 10 phần trăm; khoảng cuối gồm cả 100. */
    public static final int BUCKETS = 10;

    private ExamStatistics() {
    }

    public static BigDecimal average(Collection<BigDecimal> scores) {
        if (scores.isEmpty()) {
            return null;
        }
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    /** Tỷ lệ trong [0, 1], 4 chữ số; {@code null} khi chưa có mẫu. */
    public static BigDecimal rate(long hits, long total) {
        if (total == 0) {
            return null;
        }
        return BigDecimal.valueOf(hits).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    /** @param percentScores điểm của từng Attempt đã chấm, theo thang 0–100 như {@code exam_attempts.score} */
    public static List<ScoreBucket> distribution(Collection<BigDecimal> percentScores) {
        int[] counts = new int[BUCKETS];
        for (BigDecimal score : percentScores) {
            counts[bucketOf(score)]++;
        }
        List<ScoreBucket> buckets = new ArrayList<>(BUCKETS);
        int width = 100 / BUCKETS;
        for (int i = 0; i < BUCKETS; i++) {
            buckets.add(new ScoreBucket(i * width, (i + 1) * width, counts[i]));
        }
        return buckets;
    }

    static int bucketOf(BigDecimal percentScore) {
        if (percentScore == null || percentScore.signum() <= 0) {
            return 0;
        }
        int index = percentScore.divide(BigDecimal.valueOf(100 / BUCKETS), 0, RoundingMode.DOWN).intValue();
        return Math.min(index, BUCKETS - 1);
    }
}
