package com.example.lms.enrollment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.lms.shared.exception.ErrorCode;

class EnrollmentAccessRuleTest {

    private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");
    private static final Duration DAY = Duration.ofDays(1);

    private static Enrollment active(Instant startsAt, Instant expiresAt) {
        return Enrollment.grant(1, 2, null, EnrollmentSource.PURCHASE, startsAt, expiresAt, startsAt);
    }

    @Test
    void noEnrollment_isRequired() {
        EnrollmentAccessRule.Decision decision = EnrollmentAccessRule.evaluate(List.of(), NOW);
        assertFalse(decision.granted());
        assertEquals(ErrorCode.ENROLLMENT_REQUIRED, decision.denial());
    }

    @Test
    void activeWithinPeriod_isGranted() {
        Enrollment enrollment = active(NOW.minus(DAY), NOW.plus(DAY));
        EnrollmentAccessRule.Decision decision = EnrollmentAccessRule.evaluate(List.of(enrollment), NOW);
        assertTrue(decision.granted());
        assertSame(enrollment, decision.enrollment());
    }

    @Test
    void activeWithoutExpiry_isGranted() {
        assertTrue(EnrollmentAccessRule.evaluate(List.of(active(NOW.minus(DAY), null)), NOW).granted());
    }

    @Test
    void stillActiveInDatabaseButPastExpiry_isExpired_withoutWaitingForTheJob() {
        Enrollment lapsed = active(NOW.minus(DAY.multipliedBy(30)), NOW);
        EnrollmentAccessRule.Decision decision = EnrollmentAccessRule.evaluate(List.of(lapsed), NOW);
        assertFalse(decision.granted());
        assertEquals(ErrorCode.ENROLLMENT_EXPIRED, decision.denial());
    }

    @Test
    void revoked_isRevoked() {
        Enrollment revoked = active(NOW.minus(DAY), null);
        revoked.revoke(9, "chargeback", NOW.minusSeconds(10));
        assertEquals(ErrorCode.ENROLLMENT_REVOKED,
                EnrollmentAccessRule.evaluate(List.of(revoked), NOW).denial());
    }

    @Test
    void renewalAfterExpiry_grantsAccessFromTheNewEnrollment() {
        Enrollment old = active(NOW.minus(DAY.multipliedBy(60)), NOW.minus(DAY.multipliedBy(30)));
        old.expire(NOW.minus(DAY.multipliedBy(30)));
        Enrollment renewal = Enrollment.grant(1, 2, 77L, EnrollmentSource.RENEWAL, NOW.minus(DAY), NOW.plus(DAY),
                NOW.minus(DAY));
        EnrollmentAccessRule.Decision decision = EnrollmentAccessRule.evaluate(List.of(old, renewal), NOW);
        assertTrue(decision.granted());
        assertSame(renewal, decision.enrollment());
    }

    @Test
    void latestRecordDecidesTheDenialCode() {
        Enrollment expired = active(NOW.minus(DAY.multipliedBy(60)), NOW.minus(DAY.multipliedBy(30)));
        expired.expire(NOW.minus(DAY.multipliedBy(30)));
        Enrollment revokedLater = Enrollment.grant(1, 2, null, EnrollmentSource.ADMIN_GRANTED,
                NOW.minus(DAY.multipliedBy(10)), null, NOW.minus(DAY.multipliedBy(10)));
        revokedLater.revoke(9, "abuse", NOW.minus(DAY));
        assertEquals(ErrorCode.ENROLLMENT_REVOKED,
                EnrollmentAccessRule.evaluate(List.of(expired, revokedLater), NOW).denial());
    }
}
