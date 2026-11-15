package com.example.lms.ai.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.lms.shared.exception.BusinessException;

class HintPolicyTest {

    private static final List<String> STEPS = List.of("a", "b", "c");

    @Test
    void nextIndexAdvancesUntilLimit() {
        assertEquals(1, HintPolicy.nextIndex(STEPS, 3, 0));
        assertEquals(3, HintPolicy.nextIndex(STEPS, 3, 2));
        assertEquals(2, HintPolicy.remaining(STEPS, 3, 1));
        assertEquals(0, HintPolicy.remaining(STEPS, 3, 3));
    }

    @Test
    void limitIsMinOfConfigAndAvailableSteps() {
        assertEquals(0, HintPolicy.remaining(STEPS, 2, 2));
        assertEquals(0, HintPolicy.remaining(List.of("a"), 5, 1));
        exhausted(STEPS, 2, 2);
        exhausted(List.of(), 3, 0);
        exhausted(null, 3, 0);
        exhausted(STEPS, 0, 0);
    }

    private static void exhausted(List<String> steps, int max, long used) {
        try {
            HintPolicy.nextIndex(steps, max, used);
        } catch (BusinessException e) {
            assertEquals(422, e.httpStatus());
            return;
        }
        throw new AssertionError("expected AI_HINTS_EXHAUSTED");
    }

    @Test
    void keyValidation() {
        assertEquals("k", HintPolicy.validateKey("k"));
        for (String bad : new String[] { null, " ", "x".repeat(65) }) {
            try {
                HintPolicy.validateKey(bad);
            } catch (BusinessException e) {
                assertEquals(400, e.httpStatus());
                continue;
            }
            throw new AssertionError("expected rejection");
        }
    }
}
