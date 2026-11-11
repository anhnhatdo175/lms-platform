package com.example.lms.exam.dto;

import java.math.BigDecimal;
import java.util.List;

/** OpenAPI {@code ExamStatistics} (SHOULD); chỉ tính Attempt đã chấm. */
public record ExamStatisticsResponse(long examId, int attemptCount, int studentCount, BigDecimal averageScore,
        BigDecimal passRate, List<ScoreBucket> scoreDistribution, List<QuestionStat> questionStats) {

    public record ScoreBucket(int from, int to, int count) {
    }

    public record QuestionStat(long questionId, String content, BigDecimal correctRate, int answeredCount) {
    }
}
