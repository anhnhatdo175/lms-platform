package com.example.lms.ai.session;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.lms.ai.SessionErrorCode;

/**
 * Một vòng xử lý của worker: thu hồi lease hết hạn, nhận một phiên, tạo nhiệm vụ (ngoài transaction vì gọi dịch vụ AI
 * chậm), rồi ghi kết quả có fencing. Không bao giờ ném lỗi ra ngoài để không làm hỏng scheduler (BR-AI-11).
 */
public class SessionProcessor {

    private static final Logger log = LoggerFactory.getLogger(SessionProcessor.class);
    private static final int RECLAIM_BATCH = 50;

    private final SessionStore store;
    private final SessionTaskProducer producer;
    private final Clock clock;
    private final String workerId;

    public SessionProcessor(SessionStore store, SessionTaskProducer producer, Clock clock, String workerId) {
        this.store = store;
        this.producer = producer;
        this.clock = clock;
        this.workerId = workerId;
    }

    /** @return true nếu đã xử lý một phiên (để caller lặp tiếp), false nếu không còn việc */
    public boolean runOnce() {
        try {
            reclaimExpiredLeases();
            Instant now = clock.instant();
            Optional<SessionSnapshot> claimed = store.claimNext(now, now.plus(SessionStateMachine.LEASE), workerId);
            if (claimed.isEmpty()) {
                return false;
            }
            process(claimed.get());
            return true;
        } catch (RuntimeException e) {
            log.error("adaptive session worker iteration failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    /** Lease hết hạn tính vào auto_retry_count; hết lượt thì FAILED (Spec 20A.14). */
    void reclaimExpiredLeases() {
        Instant now = clock.instant();
        List<SessionSnapshot> expired = store.findLeaseExpired(now, RECLAIM_BATCH);
        for (SessionSnapshot s : expired) {
            handleFailure(s, SessionErrorCode.INTERNAL_ERROR, "Worker lease expired");
        }
    }

    void process(SessionSnapshot session) {
        SessionTaskProducer.Result result;
        try {
            result = producer.produce(session);
        } catch (SessionFailure failure) {
            handleFailure(session, failure.code(), failure.getMessage());
            return;
        } catch (RuntimeException e) {
            log.error("session {} producer crashed: {}", session.id(), e.getClass().getSimpleName());
            handleFailure(session, SessionErrorCode.INTERNAL_ERROR, "Unexpected error while generating tasks");
            return;
        }
        boolean written;
        try {
            written = store.complete(session.id(), session.attemptCount(), result.tasks(), result.pipelineVersion(),
                    result.retrievalTraceJson(), clock.instant());
        } catch (RuntimeException e) {
            log.error("session {} could not be completed: {}", session.id(), e.getClass().getSimpleName());
            handleFailure(session, SessionErrorCode.INTERNAL_ERROR, "Could not store generated tasks");
            return;
        }
        if (!written) {
            log.warn("session {} attempt {} lost its lease; result discarded", session.id(), session.attemptCount());
        }
    }

    private void handleFailure(SessionSnapshot session, SessionErrorCode code, String message) {
        String safe = SessionStateMachine.safeMessage(message);
        boolean applied;
        if (SessionStateMachine.canAutoRetry(code, session.autoRetryCount())) {
            Instant next = clock.instant().plus(SessionStateMachine.backoff(session.autoRetryCount()));
            applied = store.requeue(session.id(), session.attemptCount(), code, safe, next);
        } else {
            applied = store.fail(session.id(), session.attemptCount(), code, safe, clock.instant());
        }
        if (!applied) {
            log.warn("session {} attempt {} lost its lease before recording {}", session.id(), session.attemptCount(),
                    code);
        }
    }
}
