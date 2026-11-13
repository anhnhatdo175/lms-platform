package com.example.lms.commerce;

import java.util.Set;

/** Máy trạng thái Order (Spec 10.4, 10.5). */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CANCELLED,
    EXPIRED;

    /** Chuyển trạng thái thông thường (checkout, webhook, hủy, job hết hạn). */
    public boolean canTransitionTo(OrderStatus target) {
        return this == PENDING_PAYMENT && Set.of(PAID, CANCELLED, EXPIRED).contains(target);
    }

    /**
     * Ngoại lệ duy nhất của Spec 10.5: Admin đối soát chuyển {@code EXPIRED/CANCELLED → PAID}
     * khi tiền đã thực nhận. Chỉ {@code PaymentCompletionService} đi đường này, kèm lý do và audit.
     */
    public boolean canTransitionByReconciliation(OrderStatus target) {
        return canTransitionTo(target) || (target == PAID && (this == EXPIRED || this == CANCELLED));
    }
}
