package com.example.lms.shared.exception;

/**
 * Mã lỗi nghiệp vụ ổn định, sao đúng enum {@code ErrorCode} của OpenAPI v1.5.
 *
 * <p>Chủ sở hữu: A (Common response/error). Bản tạm để code của B biên dịch được; khi A đẩy bản thật
 * cùng đường dẫn này lên thì lấy bản của A;
 * A giữ nguyên danh sách mã và có thể đổi HTTP status mặc định.
 * HTTP status ở đây chỉ là mặc định; nơi ném lỗi có thể chỉ định status khác.
 */
public enum ErrorCode {
    VALIDATION_ERROR(400),
    UNAUTHENTICATED(401),
    TOKEN_EXPIRED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    STATE_CONFLICT(409),
    RATE_LIMITED(429),
    INTERNAL_ERROR(500),
    SERVICE_UNAVAILABLE(503),
    INVALID_CREDENTIALS(401),
    ACCOUNT_BLOCKED(403),
    EMAIL_ALREADY_EXISTS(409),
    RESET_TOKEN_INVALID(400),
    COURSE_NOT_EDITABLE(409),
    COURSE_NOT_PUBLISHED(422),
    REVIEW_NOT_PENDING(409),
    PREREQUISITE_CYCLE(422),
    PREREQUISITE_INVALID(422),
    CART_EMPTY(422),
    COURSE_ALREADY_ENROLLED(409),
    COUPON_INVALID(422),
    COUPON_EXPIRED(422),
    COUPON_USAGE_EXCEEDED(422),
    COUPON_NOT_APPLICABLE(422),
    IDEMPOTENCY_KEY_REUSED(409),
    ORDER_NOT_CANCELLABLE(409),
    ORDER_EXPIRED(409),
    PAYMENT_NOT_REVIEWABLE(409),
    RECONCILIATION_INVALID(409),
    ENROLLMENT_REQUIRED(403),
    ENROLLMENT_EXPIRED(403),
    ENROLLMENT_REVOKED(403),
    LESSON_NOT_PUBLISHED(403),
    LESSON_LOCKED_BY_PREREQUISITE(403),
    LESSON_LOCKED_UNTIL_RELEASE(403),
    EXAM_NOT_OPEN(422),
    EXAM_CLOSED(409),
    EXAM_NOT_READY(422),
    EXAM_ATTEMPT_LIMIT_REACHED(409),
    ATTEMPT_EXPIRED(409),
    ATTEMPT_NOT_IN_PROGRESS(409),
    QUESTION_INVALID(422),
    AI_NOT_CONFIGURED(422),
    AI_DISABLED(422),
    AI_REQUEST_OUT_OF_BOUNDS(422),
    AI_DOCUMENT_NOT_READY(409),
    AI_TASK_ALREADY_ATTEMPTED(409),
    AI_SOCRATIC_DISABLED(422),
    AI_HINTS_EXHAUSTED(409),
    NO_CANDIDATES(422),
    INSUFFICIENT_EVIDENCE(422),
    LLM_INVALID_OUTPUT(502),
    AI_UNAVAILABLE(503),
    AI_RETRY_LIMIT_REACHED(409),
    AI_HINT_NOT_AVAILABLE(409);

    private final int defaultHttpStatus;

    ErrorCode(int defaultHttpStatus) {
        this.defaultHttpStatus = defaultHttpStatus;
    }

    public int defaultHttpStatus() {
        return defaultHttpStatus;
    }
}
