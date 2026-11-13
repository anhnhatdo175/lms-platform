package com.example.lms.payment;

/** Lý do webhook UNMATCHED (Spec 11.6, dòng 1, 2, 3, 5). */
public enum UnmatchedReason {
    NO_REFERENCE,
    UNKNOWN_REFERENCE,
    ALREADY_PAID,
    PAYMENT_NOT_PENDING
}
