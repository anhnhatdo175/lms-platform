package com.example.lms.payment;

import java.util.Map;
import java.util.Set;

/** Máy trạng thái Payment (Spec 11.1): đúng 6 cặp chuyển hợp lệ, PAID và FAILED là trạng thái cuối. */
public enum PaymentStatus {
    PENDING,
    PAID,
    REVIEW_REQUIRED,
    FAILED,
    CANCELLED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = Map.of(
            PENDING, Set.of(PAID, REVIEW_REQUIRED, CANCELLED),
            CANCELLED, Set.of(REVIEW_REQUIRED),
            REVIEW_REQUIRED, Set.of(PAID, FAILED),
            PAID, Set.of(),
            FAILED, Set.of());

    public boolean canTransitionTo(PaymentStatus target) {
        return ALLOWED.get(this).contains(target);
    }
}
