package com.example.lms.ai.session;

import java.time.Duration;

import com.example.lms.ai.SessionErrorCode;
import com.example.lms.ai.SessionStatus;

/**
 * Quy tắc thuần của máy trạng thái phiên (Spec 20A.14, AC-AI-15); không có I/O nên test được trực tiếp.
 *
 * <pre>
 * PENDING -> PROCESSING -> SUCCEEDED | FAILED
 * PROCESSING -> PENDING  (tự retry: lỗi tạm thời hoặc hết lease)
 * FAILED -> PENDING      (retry thủ công)
 * </pre>
 */
public final class SessionStateMachine {

    public static final int MAX_AUTO_RETRIES = 3;
    public static final int MAX_MANUAL_RETRIES = 3;
    public static final Duration LEASE = Duration.ofMinutes(5);
    private static final Duration BACKOFF_BASE = Duration.ofSeconds(30);
    private static final Duration BACKOFF_MAX = Duration.ofMinutes(10);

    private SessionStateMachine() {
    }

    /** Lỗi tạm thời được tự retry; lỗi nghiệp vụ thì FAILED ngay. */
    public static boolean isTransient(SessionErrorCode code) {
        return switch (code) {
            case AI_UNAVAILABLE, LLM_INVALID_OUTPUT, INTERNAL_ERROR -> true;
            case AI_NOT_CONFIGURED, NO_CANDIDATES, INSUFFICIENT_EVIDENCE -> false;
        };
    }

    public static boolean canAutoRetry(SessionErrorCode code, int autoRetryCount) {
        return isTransient(code) && autoRetryCount < MAX_AUTO_RETRIES;
    }

    /** Backoff mũ: 30s, 60s, 120s, ... tối đa 10 phút; {@code autoRetryCount} là số lần đã retry trước đó. */
    public static Duration backoff(int autoRetryCount) {
        int shift = Math.max(0, Math.min(autoRetryCount, 10));
        Duration delay = BACKOFF_BASE.multipliedBy(1L << shift);
        return delay.compareTo(BACKOFF_MAX) > 0 ? BACKOFF_MAX : delay;
    }

    public static boolean canManualRetry(SessionStatus status, int manualRetryCount) {
        return status == SessionStatus.FAILED && manualRetryCount < MAX_MANUAL_RETRIES;
    }

    public static boolean isTerminal(SessionStatus status) {
        return status == SessionStatus.SUCCEEDED;
    }

    /** Thông điệp lỗi lưu DB: cắt ngắn, không chứa ngoại lệ thô (không lộ bí mật, ERD 17.14). */
    public static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String oneLine = message.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 500 ? oneLine : oneLine.substring(0, 500);
    }
}
