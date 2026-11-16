package com.example.lms.ai;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * Các UPDATE có điều kiện {@code status} + {@code attemptCount} là cơ chế fencing của Spec 20A.14: trả 0 dòng nghĩa là
 * lease đã chuyển giao (hoặc phiên đã đổi trạng thái) và worker phải dừng.
 */
public interface AdaptiveTaskSessionRepository extends JpaRepository<AdaptiveTaskSession, Long> {

    Optional<AdaptiveTaskSession> findByStudentIdAndClientRequestId(long studentId, String clientRequestId);

    Page<AdaptiveTaskSession> findByStudentId(long studentId, Pageable pageable);

    @Query("""
            SELECT s FROM AdaptiveTaskSession s
            WHERE s.status = com.example.lms.ai.SessionStatus.PENDING
              AND (s.nextRetryAt IS NULL OR s.nextRetryAt <= :now)
            ORDER BY s.id
            """)
    List<AdaptiveTaskSession> findClaimable(Instant now, Pageable pageable);

    @Query("""
            SELECT s FROM AdaptiveTaskSession s
            WHERE s.status = com.example.lms.ai.SessionStatus.PROCESSING AND s.leaseExpiresAt <= :now
            ORDER BY s.id
            """)
    List<AdaptiveTaskSession> findLeaseExpired(Instant now, Pageable pageable);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AdaptiveTaskSession s
            SET s.status = com.example.lms.ai.SessionStatus.PROCESSING, s.attemptCount = s.attemptCount + 1,
                s.leaseExpiresAt = :lease, s.workerId = :worker
            WHERE s.id = :id AND s.status = com.example.lms.ai.SessionStatus.PENDING AND s.attemptCount = :seenAttempt
            """)
    int claim(long id, int seenAttempt, Instant lease, String worker);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AdaptiveTaskSession s SET s.leaseExpiresAt = :lease
            WHERE s.id = :id AND s.status = com.example.lms.ai.SessionStatus.PROCESSING AND s.attemptCount = :attempt
            """)
    int extendLease(long id, int attempt, Instant lease);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AdaptiveTaskSession s
            SET s.status = com.example.lms.ai.SessionStatus.SUCCEEDED, s.completedAt = :now, s.leaseExpiresAt = NULL,
                s.nextRetryAt = NULL, s.errorCode = NULL, s.pipelineVersion = :pipeline, s.retrievalTraceJson = :trace
            WHERE s.id = :id AND s.status = com.example.lms.ai.SessionStatus.PROCESSING AND s.attemptCount = :attempt
            """)
    int markSucceeded(long id, int attempt, Instant now, String pipeline, String trace);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AdaptiveTaskSession s
            SET s.status = com.example.lms.ai.SessionStatus.FAILED, s.completedAt = :now, s.leaseExpiresAt = NULL,
                s.nextRetryAt = NULL, s.errorCode = :code, s.lastErrorMessage = :message
            WHERE s.id = :id AND s.status = com.example.lms.ai.SessionStatus.PROCESSING AND s.attemptCount = :attempt
            """)
    int markFailed(long id, int attempt, Instant now, SessionErrorCode code, String message);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AdaptiveTaskSession s
            SET s.status = com.example.lms.ai.SessionStatus.PENDING, s.autoRetryCount = s.autoRetryCount + 1,
                s.leaseExpiresAt = NULL, s.nextRetryAt = :next, s.errorCode = :code, s.lastErrorMessage = :message
            WHERE s.id = :id AND s.status = com.example.lms.ai.SessionStatus.PROCESSING AND s.attemptCount = :attempt
            """)
    int markRequeued(long id, int attempt, Instant next, SessionErrorCode code, String message);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AdaptiveTaskSession s
            SET s.status = com.example.lms.ai.SessionStatus.PENDING, s.manualRetryCount = s.manualRetryCount + 1,
                s.autoRetryCount = 0, s.nextRetryAt = NULL, s.completedAt = NULL
            WHERE s.id = :id AND s.status = com.example.lms.ai.SessionStatus.FAILED AND s.manualRetryCount < :max
            """)
    int markManualRetry(long id, int max);
}
