package com.example.lms.ai.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.lms.ai.AdaptiveTaskType;
import com.example.lms.ai.SessionErrorCode;
import com.example.lms.ai.SessionStatus;
import com.example.lms.ai.TaskOrigin;
import com.example.lms.ai.ValidationStatus;
import com.example.lms.contract.question.Difficulty;

/** AC-AI-15: phân loại lỗi, giới hạn retry, lease + fencing, ghi nhiệm vụ một lần. */
class SessionStateMachineTest {

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-15T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static TaskDraft draft(int position) {
        return new TaskDraft(position, TaskOrigin.GENERATED, null, AdaptiveTaskType.SHORT_ANSWER, "frac",
                Difficulty.EASY, "{}", "{}", null, "reason", null, null, null, ValidationStatus.VALID);
    }

    private final MutableClock clock = new MutableClock();
    private final InMemorySessionStore store = new InMemorySessionStore();

    private SessionProcessor processor(SessionTaskProducer producer, String worker) {
        return new SessionProcessor(store, producer, clock, worker);
    }

    private static SessionTaskProducer ok() {
        return s -> new SessionTaskProducer.Result(List.of(draft(1), draft(2)), "pipe-1", "{}");
    }

    private static SessionTaskProducer failing(SessionErrorCode code) {
        return s -> {
            throw new SessionFailure(code, "boom");
        };
    }

    // ---------- rules ----------
    @Test
    void errorClassification() {
        assertTrue(SessionStateMachine.isTransient(SessionErrorCode.AI_UNAVAILABLE));
        assertTrue(SessionStateMachine.isTransient(SessionErrorCode.LLM_INVALID_OUTPUT));
        assertTrue(SessionStateMachine.isTransient(SessionErrorCode.INTERNAL_ERROR));
        assertFalse(SessionStateMachine.isTransient(SessionErrorCode.AI_NOT_CONFIGURED));
        assertFalse(SessionStateMachine.isTransient(SessionErrorCode.NO_CANDIDATES));
        assertFalse(SessionStateMachine.isTransient(SessionErrorCode.INSUFFICIENT_EVIDENCE));
    }

    @Test
    void backoffDoublesAndCaps() {
        assertEquals(Duration.ofSeconds(30), SessionStateMachine.backoff(0));
        assertEquals(Duration.ofSeconds(60), SessionStateMachine.backoff(1));
        assertEquals(Duration.ofSeconds(120), SessionStateMachine.backoff(2));
        assertEquals(Duration.ofMinutes(10), SessionStateMachine.backoff(9));
        assertEquals(Duration.ofMinutes(10), SessionStateMachine.backoff(500));
    }

    @Test
    void manualRetryRules() {
        assertTrue(SessionStateMachine.canManualRetry(SessionStatus.FAILED, 2));
        assertFalse(SessionStateMachine.canManualRetry(SessionStatus.FAILED, 3));
        assertFalse(SessionStateMachine.canManualRetry(SessionStatus.SUCCEEDED, 0));
        assertFalse(SessionStateMachine.canManualRetry(SessionStatus.PENDING, 0));
        assertFalse(SessionStateMachine.canManualRetry(SessionStatus.PROCESSING, 0));
    }

    @Test
    void safeMessageIsTruncatedAndSingleLine() {
        assertNull(SessionStateMachine.safeMessage("  "));
        assertEquals("a b", SessionStateMachine.safeMessage("a\n  b"));
        assertEquals(500, SessionStateMachine.safeMessage("x".repeat(900)).length());
    }

    // ---------- processing ----------
    @Test
    void successWritesTasksOnceAndSucceeds() {
        var row = store.add();
        assertTrue(processor(ok(), "w1").runOnce());
        assertEquals(SessionStatus.SUCCEEDED, row.status);
        assertEquals(2, row.tasks.size());
        assertEquals(1, row.taskWrites);
        assertEquals(1, row.attemptCount);
        assertFalse(processor(ok(), "w1").runOnce());
    }

    @Test
    void businessErrorsFailImmediatelyWithoutRetry() {
        for (SessionErrorCode code : List.of(SessionErrorCode.AI_NOT_CONFIGURED, SessionErrorCode.NO_CANDIDATES,
                SessionErrorCode.INSUFFICIENT_EVIDENCE)) {
            var row = store.add();
            processor(failing(code), "w1").runOnce();
            assertEquals(SessionStatus.FAILED, row.status);
            assertEquals(code, row.errorCode);
            assertEquals(0, row.autoRetryCount);
        }
    }

    @Test
    void transientErrorRetriesThreeTimesThenFails() {
        var row = store.add();
        var p = processor(failing(SessionErrorCode.AI_UNAVAILABLE), "w1");
        for (int i = 1; i <= 3; i++) {
            assertTrue(p.runOnce());
            assertEquals(SessionStatus.PENDING, row.status);
            assertEquals(i, row.autoRetryCount);
            assertFalse(p.runOnce(), "backoff: not due yet");
            clock.advance(Duration.ofMinutes(11));
        }
        assertTrue(p.runOnce());
        assertEquals(SessionStatus.FAILED, row.status);
        assertEquals(SessionErrorCode.AI_UNAVAILABLE, row.errorCode);
        assertEquals(4, row.attemptCount);
        assertEquals(3, row.autoRetryCount);
    }

    @Test
    void unexpectedProducerExceptionIsInternalErrorAndRetried() {
        var row = store.add();
        processor(s -> {
            throw new IllegalStateException("secret detail");
        }, "w1").runOnce();
        assertEquals(SessionStatus.PENDING, row.status);
        assertEquals(SessionErrorCode.INTERNAL_ERROR, row.errorCode);
        assertFalse(row.lastErrorMessage.contains("secret"));
    }

    @Test
    void expiredLeaseIsReclaimedAndCountsAsAutoRetry() {
        var row = store.add();
        store.claimNext(clock.instant(), clock.instant().plus(SessionStateMachine.LEASE), "dead-worker");
        assertEquals(SessionStatus.PROCESSING, row.status);
        clock.advance(SessionStateMachine.LEASE.plusSeconds(1));
        processor(ok(), "w2").reclaimExpiredLeases();
        assertEquals(SessionStatus.PENDING, row.status);
        assertEquals(1, row.autoRetryCount);
    }

    @Test
    void repeatedLeaseExpiryEventuallyFails() {
        var row = store.add();
        var p = processor(ok(), "w2");
        for (int i = 0; i < 4; i++) {
            clock.advance(Duration.ofMinutes(11));
            store.claimNext(clock.instant(), clock.instant().plus(SessionStateMachine.LEASE), "dead");
            clock.advance(SessionStateMachine.LEASE.plusSeconds(1));
            p.reclaimExpiredLeases();
        }
        assertEquals(SessionStatus.FAILED, row.status);
        assertEquals(3, row.autoRetryCount);
    }

    @Test
    void lateWorkerAfterLeaseHandoverCannotOverwriteOrDuplicate() {
        var row = store.add();
        SessionSnapshot a = store.claimNext(clock.instant(), clock.instant().plus(SessionStateMachine.LEASE), "A")
                .orElseThrow();
        clock.advance(SessionStateMachine.LEASE.plusSeconds(1));
        // worker B: reclaims the expired lease, waits out the backoff, claims attempt 2 and finishes
        var b = processor(ok(), "B");
        b.reclaimExpiredLeases();
        clock.advance(Duration.ofMinutes(11));
        assertTrue(b.runOnce());
        assertEquals(SessionStatus.SUCCEEDED, row.status);
        assertEquals(2, row.attemptCount);
        // worker A wakes up late with the stale attempt number: everything it writes is rejected
        processor((s) -> new SessionTaskProducer.Result(List.of(draft(1), draft(2), draft(3)), "late", "{}"), "A")
                .process(a);
        assertEquals(1, row.taskWrites);
        assertEquals(2, row.tasks.size());
        assertEquals(SessionStatus.SUCCEEDED, row.status);
    }

    @Test
    void lateWorkerFailureDoesNotDowngradeSucceededSession() {
        var row = store.add();
        SessionSnapshot a = store.claimNext(clock.instant(), clock.instant().plus(SessionStateMachine.LEASE), "A")
                .orElseThrow();
        clock.advance(SessionStateMachine.LEASE.plusSeconds(1));
        var b = processor(ok(), "B");
        b.reclaimExpiredLeases();
        clock.advance(Duration.ofMinutes(11));
        b.runOnce();
        processor(failing(SessionErrorCode.NO_CANDIDATES), "A").process(a);
        assertEquals(SessionStatus.SUCCEEDED, row.status);
    }

    @Test
    void manualRetryLimitedToThree() {
        var row = store.add();
        processor(failing(SessionErrorCode.NO_CANDIDATES), "w").runOnce();
        for (int i = 1; i <= 3; i++) {
            assertTrue(store.manualRetry(row.id, SessionStateMachine.MAX_MANUAL_RETRIES));
            assertEquals(SessionStatus.PENDING, row.status);
            assertEquals(0, row.autoRetryCount);
            processor(failing(SessionErrorCode.NO_CANDIDATES), "w").runOnce();
        }
        assertFalse(store.manualRetry(row.id, SessionStateMachine.MAX_MANUAL_RETRIES));
        assertEquals(3, row.manualRetryCount);
    }

    @Test
    void succeededSessionCannotBeRetried() {
        var row = store.add();
        processor(ok(), "w").runOnce();
        assertFalse(store.manualRetry(row.id, SessionStateMachine.MAX_MANUAL_RETRIES));
        assertEquals(SessionStatus.SUCCEEDED, row.status);
    }
}
