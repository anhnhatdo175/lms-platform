package com.example.lms.exam;

/**
 * Spec 14.3. Không có EXPIRED: hết giờ là {@code GRADED} + {@code AUTO_EXPIRED} (DEC-010).
 * Chấm chạy đồng bộ nên Attempt đi thẳng IN_PROGRESS → GRADED; SUBMITTED dành cho chấm bất đồng bộ về sau.
 */
public enum AttemptStatus {
    IN_PROGRESS,
    SUBMITTED,
    GRADED;

    public boolean canTransitionTo(AttemptStatus target) {
        return (this == IN_PROGRESS && (target == SUBMITTED || target == GRADED))
                || (this == SUBMITTED && target == GRADED);
    }
}
