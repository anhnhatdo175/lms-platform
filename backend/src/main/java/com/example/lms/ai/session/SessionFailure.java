package com.example.lms.ai.session;

import com.example.lms.ai.SessionErrorCode;

/** Producer ném khi không thể tạo nhiệm vụ; {@code code} quyết định có tự retry hay không. */
public class SessionFailure extends RuntimeException {

    private final SessionErrorCode code;

    public SessionFailure(SessionErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public SessionFailure(SessionErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public SessionErrorCode code() {
        return code;
    }
}
