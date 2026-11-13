package com.example.lms.shared.exception;

/**
 * Lỗi nghiệp vụ mang mã ổn định; bộ xử lý lỗi chung (của A) chuyển thành {@code ErrorResponse} có {@code traceId}.
 *
 * <p>Chủ sở hữu: A. Bản tạm với chữ ký tối thiểu để code của B biên dịch được; lấy bản của A khi có.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final int httpStatus;

    public BusinessException(ErrorCode code, String message) {
        this(code, code.defaultHttpStatus(), message);
    }

    public BusinessException(ErrorCode code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public ErrorCode code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    /** Tài nguyên riêng tư của người khác cũng trả 404 để không lộ sự tồn tại (Spec 7.4). */
    public static BusinessException notFound(String what) {
        return new BusinessException(ErrorCode.NOT_FOUND, what + " not found");
    }

    public static BusinessException stateConflict(String message) {
        return new BusinessException(ErrorCode.STATE_CONFLICT, message);
    }
}
