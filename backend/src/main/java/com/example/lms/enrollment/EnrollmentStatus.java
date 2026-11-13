package com.example.lms.enrollment;

/** Enrollment tạo ra là ACTIVE, không có PENDING (Spec 12.2, 12.3). */
public enum EnrollmentStatus {
    ACTIVE,
    EXPIRED,
    REVOKED;

    public boolean canTransitionTo(EnrollmentStatus target) {
        return this == ACTIVE && (target == EXPIRED || target == REVOKED);
    }
}
