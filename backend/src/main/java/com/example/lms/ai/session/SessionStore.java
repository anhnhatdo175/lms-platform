package com.example.lms.ai.session;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.example.lms.ai.SessionErrorCode;

/**
 * Cổng lưu trữ cho worker. Mọi thao tác ghi đều có fencing: chỉ thành công khi phiên còn {@code PROCESSING} với đúng
 * {@code attempt} đã nhận, nên worker đến muộn sau khi lease chuyển giao không ghi đè được (trả {@code false}).
 */
public interface SessionStore {

    /** PENDING đến hạn -> PROCESSING, tăng attempt_count, đặt lease. Trả rỗng nếu không có việc hoặc thua cuộc đua. */
    Optional<SessionSnapshot> claimNext(Instant now, Instant leaseExpiresAt, String workerId);

    /** Phiên PROCESSING đã quá lease. */
    List<SessionSnapshot> findLeaseExpired(Instant now, int limit);

    /** Ghi toàn bộ nhiệm vụ và chuyển SUCCEEDED trong một transaction. */
    boolean complete(long sessionId, int attempt, List<TaskDraft> tasks, String pipelineVersion,
            String retrievalTraceJson, Instant now);

    /** PROCESSING -> FAILED. */
    boolean fail(long sessionId, int attempt, SessionErrorCode code, String message, Instant now);

    /** PROCESSING -> PENDING, auto_retry_count + 1, đặt next_retry_at. */
    boolean requeue(long sessionId, int attempt, SessionErrorCode code, String message, Instant nextRetryAt);

    /** Gia hạn lease khi worker còn giữ đúng {@code attempt}; false nghĩa là lease đã mất. */
    default boolean extendLease(long sessionId, int attempt, Instant leaseExpiresAt) {
        return true;
    }

    /** FAILED -> PENDING nếu còn lượt retry thủ công; đặt lại auto_retry_count, giữ last_error_*. */
    boolean manualRetry(long sessionId, int maxManualRetries);
}
