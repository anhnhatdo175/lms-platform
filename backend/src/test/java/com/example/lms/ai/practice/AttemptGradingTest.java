package com.example.lms.ai.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.lms.ai.practice.AttemptViews.AnswerPayload;
import com.example.lms.ai.practice.AttemptViews.SubmitRequest;
import com.example.lms.shared.exception.BusinessException;

class AttemptGradingTest {

    private static final String UUID1 = "123e4567-e89b-12d3-a456-426614174000";

    @Test
    void exactIgnoresCaseAndWhitespace() {
        assertTrue(RuleBasedGrader.grade("exact", "Hà Nội", "  hà   nội ").correct());
        assertFalse(RuleBasedGrader.grade("exact", "Hà Nội", "Huế").correct());
        assertTrue(RuleBasedGrader.grade("exact", "5/7", " 5 / 7 ").correct());
        assertFalse(RuleBasedGrader.grade("exact", "5/7", "10/14").correct());
    }

    @Test
    void numericAcceptsDecimalCommaAndRejectsGarbage() {
        assertTrue(RuleBasedGrader.grade("numeric", "0.5", "0,5").correct());
        assertTrue(RuleBasedGrader.grade("numeric", 3, "3.0").correct());
        assertFalse(RuleBasedGrader.grade("numeric", "0.5", "abc").correct());
        assertFalse(RuleBasedGrader.grade("numeric", "1000", "1,000.5").correct());
        assertFalse(RuleBasedGrader.grade("numeric", "2", "NaN").correct());
    }

    @Test
    void otherTypesNeedReview() {
        RuleBasedGrader.Grade g = RuleBasedGrader.grade("rubric", "x", "x");
        assertFalse(g.gradable());
        assertNull(g.correct());
        assertFalse(RuleBasedGrader.grade("exact", null, "x").gradable());
    }

    @Test
    void numericAnswersDisplayWithoutTrailingZeros() {
        assertEquals("37", AdaptiveAttemptService.displayAnswer(37.0));
        assertEquals("2.6", AdaptiveAttemptService.displayAnswer(2.60));
        assertEquals("144000", AdaptiveAttemptService.displayAnswer(144000L));
        assertEquals("5/7", AdaptiveAttemptService.displayAnswer("5/7"));
        assertNull(AdaptiveAttemptService.displayAnswer(null));
    }

    private static void rejects(SubmitRequest r, int status) {
        try {
            AdaptiveAttemptService.validate(r);
        } catch (BusinessException e) {
            assertEquals(status, e.httpStatus());
            return;
        }
        throw new AssertionError("expected rejection");
    }

    @Test
    void validatesRequest() {
        assertEquals("a", AdaptiveAttemptService.validate(new SubmitRequest(new AnswerPayload(null, "a"), 5L, UUID1)));
        rejects(new SubmitRequest(null, 5L, UUID1), 400);
        rejects(new SubmitRequest(new AnswerPayload(null, "a"), -1L, UUID1), 400);
        rejects(new SubmitRequest(new AnswerPayload(null, "a"), 5L, "not-a-uuid"), 400);
        rejects(new SubmitRequest(new AnswerPayload("o1", "a"), 5L, UUID1), 422);
        rejects(new SubmitRequest(new AnswerPayload("o1", null), 5L, UUID1), 422);
        rejects(new SubmitRequest(new AnswerPayload(null, " "), 5L, UUID1), 422);
    }
}
