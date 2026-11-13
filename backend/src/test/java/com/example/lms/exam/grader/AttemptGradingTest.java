package com.example.lms.exam.grader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.lms.contract.question.QuestionType;
import com.example.lms.exam.grader.AttemptGrading.QuestionSnapshot;
import com.example.lms.exam.snapshot.OptionSnapshot;

class AttemptGradingTest {

    private final QuestionGraderResolver graders = new QuestionGraderResolver(
            List.of(new SingleChoiceGrader(), new MultipleChoiceGrader()));

    private final List<QuestionSnapshot> questions = List.of(
            new QuestionSnapshot(101, QuestionType.SINGLE_CHOICE, new BigDecimal("1.00"),
                    List.of(new OptionSnapshot(1, "a", true), new OptionSnapshot(2, "b", false))),
            new QuestionSnapshot(102, QuestionType.MULTIPLE_CHOICE, new BigDecimal("2.00"),
                    List.of(new OptionSnapshot(3, "c", true), new OptionSnapshot(4, "d", true),
                            new OptionSnapshot(5, "e", false))),
            new QuestionSnapshot(103, QuestionType.SINGLE_CHOICE, new BigDecimal("1.00"),
                    List.of(new OptionSnapshot(6, "f", false), new OptionSnapshot(7, "g", true))));

    @Test
    void gradesFromSnapshot_blankCountsAsWrong() {
        AttemptGrading.Result r = AttemptGrading.grade(questions,
                Map.of(101L, Set.of(1L), 102L, Set.of(3L)), new BigDecimal("4.00"), new BigDecimal("50"), graders);
        assertTrue(r.byAttemptQuestion().get(101L).correct());
        assertFalse(r.byAttemptQuestion().get(102L).correct(), "multiple choice has no partial credit");
        assertFalse(r.byAttemptQuestion().get(103L).correct(), "blank");
        assertEquals(new BigDecimal("1.00"), r.awarded());
        assertEquals(new BigDecimal("25.00"), r.score());
        assertFalse(r.passed());
    }

    @Test
    void passesAtExactlyThePassingScore() {
        AttemptGrading.Result r = AttemptGrading.grade(questions,
                Map.of(101L, Set.of(1L), 103L, Set.of(7L)), new BigDecimal("4.00"), new BigDecimal("50.00"), graders);
        assertEquals(new BigDecimal("50.00"), r.score());
        assertTrue(r.passed());
    }

    @Test
    void fullMarks() {
        AttemptGrading.Result r = AttemptGrading.grade(questions,
                Map.of(101L, Set.of(1L), 102L, Set.of(4L, 3L), 103L, Set.of(7L)), new BigDecimal("4.00"),
                new BigDecimal("80"), graders);
        assertEquals(new BigDecimal("100.00"), r.score());
        assertEquals(3, r.byAttemptQuestion().size());
        assertTrue(r.passed());
    }

    @Test
    void zeroMaximumIsADataError() {
        assertThrows(IllegalStateException.class,
                () -> AttemptGrading.grade(List.of(), Map.of(), BigDecimal.ZERO, BigDecimal.TEN, graders));
    }
}
