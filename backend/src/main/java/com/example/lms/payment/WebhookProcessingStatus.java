package com.example.lms.payment;

/** Trạng thái xử lý webhook (Spec 11.2). */
public enum WebhookProcessingStatus {
    RECEIVED,
    VALIDATED,
    MATCHED,
    PROCESSED,
    DUPLICATE,
    UNMATCHED,
    FAILED;

    /** Đã xử lý xong; log ở trạng thái khác (RECEIVED, VALIDATED) nghĩa là lần xử lý trước bị gián đoạn. */
    public boolean isFinished() {
        return this != RECEIVED && this != VALIDATED;
    }
}
