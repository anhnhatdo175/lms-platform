package com.example.lms.enrollment;

public enum EnrollmentSource {
    PURCHASE,
    ADMIN_GRANTED,
    /** Mua lại Course đã từng có Enrollment hết hạn (Spec 12.4). */
    RENEWAL
}
