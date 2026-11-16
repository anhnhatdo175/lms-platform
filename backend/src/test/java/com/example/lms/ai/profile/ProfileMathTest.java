package com.example.lms.ai.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ProfileMathTest {

    private static final double EPS = 1e-9;

    @Test
    void firstEvidenceStartsFromNeutralNotFromStoredDefault() {
        // 0.7*0.5 + 0.3*1 = 0.65 dù giá trị lưu là gì
        assertEquals(0.65, ProfileMath.nextMastery(0.123, 0, 1.0), EPS);
        assertEquals(0.35, ProfileMath.nextMastery(0.9, 0, 0.0), EPS);
    }

    @Test
    void emaUsesAlphaPointThree() {
        assertEquals(0.7 * 0.6 + 0.3 * 1.0, ProfileMath.nextMastery(0.6, 5, 1.0), EPS);
        assertEquals(0.7 * 0.6, ProfileMath.nextMastery(0.6, 5, 0.0), EPS);
    }

    @Test
    void masteryStaysInUnitInterval() {
        double m = 0.5;
        for (int i = 0; i < 200; i++) {
            m = ProfileMath.nextMastery(m, i, 1.0);
            assertTrue(m >= 0 && m <= 1);
        }
        assertTrue(m > 0.99);
    }

    @Test
    void outcomeUsesScoreRatioElseCorrectFlag() {
        assertEquals(0.5, ProfileMath.outcome(new BigDecimal("1"), new BigDecimal("2"), null, 0), EPS);
        assertEquals(1.0, ProfileMath.outcome(null, null, true, 0), EPS);
        assertEquals(0.0, ProfileMath.outcome(null, null, false, 0), EPS);
        assertEquals(0.0, ProfileMath.outcome(null, null, null, 0), EPS);
    }

    @Test
    void hintsReduceACorrectOutcomeButNeverBelowZero() {
        assertEquals(0.7, ProfileMath.outcome(null, null, true, 1), EPS);
        assertEquals(0.4, ProfileMath.outcome(null, null, true, 2), 1e-9);
        assertEquals(0.0, ProfileMath.outcome(null, null, true, 4), EPS);
        assertEquals(0.0, ProfileMath.outcome(null, null, false, 2), EPS);
    }

    @Test
    void scaleRoundsToFiveDecimals() {
        assertEquals(new BigDecimal("0.65000"), ProfileMath.scale(0.65));
        assertEquals(new BigDecimal("1.00000"), ProfileMath.scale(1.7));
    }
}
