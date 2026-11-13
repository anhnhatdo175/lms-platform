package com.example.lms.exam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.lms.contract.question.Difficulty;
import com.example.lms.contract.question.QuestionQueryService.OptionView;
import com.example.lms.contract.question.QuestionQueryService.QuestionView;
import com.example.lms.contract.question.QuestionType;
import com.example.lms.exam.dto.ExamStatisticsResponse.ScoreBucket;

class ExamPureRulesTest {

    private static final Instant T0 = Instant.parse("2026-11-10T08:00:00Z");

    @Test
    void attemptWindow_isEarlierOfDurationAndExamEnd() {
        assertEquals(T0.plusSeconds(3600), AttemptWindow.expiresAt(T0, 60, null));
        assertEquals(T0.plusSeconds(3600), AttemptWindow.expiresAt(T0, 60, T0.plusSeconds(7200)));
        assertEquals(T0.plusSeconds(600), AttemptWindow.expiresAt(T0, 60, T0.plusSeconds(600)));
    }

    @Test
    void resultVisibility() {
        assertEquals(new ResultVisibility(true, false),
                ResultVisibility.of(ResultDisplayMode.IMMEDIATE_SCORE_ONLY, false));
        assertEquals(new ResultVisibility(true, true),
                ResultVisibility.of(ResultDisplayMode.IMMEDIATE_WITH_CORRECT_ANSWERS, false));
        assertEquals(new ResultVisibility(false, false), ResultVisibility.of(ResultDisplayMode.AFTER_EXAM_CLOSED, false));
        assertEquals(new ResultVisibility(true, true), ResultVisibility.of(ResultDisplayMode.AFTER_EXAM_CLOSED, true));
    }

    @Test
    void questionRules() {
        OptionView right = new OptionView(1, "a", true, 1);
        OptionView right2 = new OptionView(2, "b", true, 2);
        OptionView wrong = new OptionView(3, "c", false, 3);
        assertTrue(QuestionRules.isGradable(q(QuestionType.SINGLE_CHOICE, true, right, wrong)));
        assertFalse(QuestionRules.isGradable(q(QuestionType.SINGLE_CHOICE, true, right, right2)));
        assertFalse(QuestionRules.isGradable(q(QuestionType.SINGLE_CHOICE, true, wrong)));
        assertTrue(QuestionRules.isGradable(q(QuestionType.MULTIPLE_CHOICE, true, right, right2, wrong)));
        assertFalse(QuestionRules.isGradable(q(QuestionType.MULTIPLE_CHOICE, true, wrong)));
        assertFalse(QuestionRules.isGradable(q(QuestionType.MULTIPLE_CHOICE, true)));
        assertFalse(QuestionRules.isGradable(q(QuestionType.SINGLE_CHOICE, false, right, wrong)));
    }

    @Test
    void statistics_averageAndRate() {
        assertNull(ExamStatistics.average(List.of()));
        assertEquals(new BigDecimal("66.67"),
                ExamStatistics.average(List.of(new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("50"))));
        assertNull(ExamStatistics.rate(0, 0));
        assertEquals(new BigDecimal("0.3333"), ExamStatistics.rate(1, 3));
        assertEquals(new BigDecimal("1.0000"), ExamStatistics.rate(4, 4));
    }

    @Test
    void statistics_bucketsOfTenPoints_lastBucketIncludes100() {
        assertEquals(0, ExamStatistics.bucketOf(BigDecimal.ZERO));
        assertEquals(0, ExamStatistics.bucketOf(null));
        assertEquals(0, ExamStatistics.bucketOf(new BigDecimal("9.99")));
        assertEquals(1, ExamStatistics.bucketOf(new BigDecimal("10.00")));
        assertEquals(9, ExamStatistics.bucketOf(new BigDecimal("99.99")));
        assertEquals(9, ExamStatistics.bucketOf(new BigDecimal("100.00")));

        List<ScoreBucket> buckets = ExamStatistics.distribution(
                List.of(new BigDecimal("100.00"), new BigDecimal("55.00"), new BigDecimal("59.50")));
        assertEquals(10, buckets.size());
        assertEquals(new ScoreBucket(50, 60, 2), buckets.get(5));
        assertEquals(new ScoreBucket(90, 100, 1), buckets.get(9));
        assertEquals(3, buckets.stream().mapToInt(ScoreBucket::count).sum());
    }

    private static QuestionView q(QuestionType type, boolean active, OptionView... options) {
        return new QuestionView(1, 1, type, Difficulty.EASY, "S1", "content", "why", active, List.of(options));
    }
}
