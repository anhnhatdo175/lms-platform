package com.example.lms.exam.grader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.lms.contract.question.QuestionType;

class GraderTest {

    private final SingleChoiceGrader single = new SingleChoiceGrader();
    private final MultipleChoiceGrader multiple = new MultipleChoiceGrader();

    @Test
    void gradersDeclareTheirType() {
        assertEquals(QuestionType.SINGLE_CHOICE, single.supports());
        assertEquals(QuestionType.MULTIPLE_CHOICE, multiple.supports());
    }

    @Test
    void singleChoice() {
        assertTrue(single.isCorrect(Set.of(10L), Set.of(10L)));
        assertFalse(single.isCorrect(Set.of(10L), Set.of(11L)));
        assertFalse(single.isCorrect(Set.of(10L), Set.of()));
        assertFalse(single.isCorrect(Set.of(10L), Set.of(10L, 11L)));
    }

    @Test
    void multipleChoice_isExactMatch_noPartialCredit() {
        Set<Long> correct = Set.of(1L, 2L);
        assertTrue(multiple.isCorrect(correct, Set.of(2L, 1L)));
        assertFalse(multiple.isCorrect(correct, Set.of(1L)));
        assertFalse(multiple.isCorrect(correct, Set.of(1L, 2L, 3L)));
        assertFalse(multiple.isCorrect(correct, Set.of()));
    }

    @Test
    void score_isPercentRoundedHalfUp() {
        assertEquals(new BigDecimal("66.67"), AttemptScoring.percent(new BigDecimal("2"), new BigDecimal("3")));
        assertEquals(new BigDecimal("100.00"), AttemptScoring.percent(new BigDecimal("7.5"), new BigDecimal("7.5")));
        assertEquals(new BigDecimal("0.00"), AttemptScoring.percent(BigDecimal.ZERO, BigDecimal.TEN));
        assertThrows(IllegalStateException.class, () -> AttemptScoring.percent(BigDecimal.ONE, BigDecimal.ZERO));
    }

    @Test
    void passed_atExactlyThePassingScore() {
        assertTrue(AttemptScoring.passed(new BigDecimal("50.00"), new BigDecimal("50")));
        assertFalse(AttemptScoring.passed(new BigDecimal("49.99"), new BigDecimal("50")));
    }
}
