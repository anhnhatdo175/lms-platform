package com.example.lms.payment;

/** Lý do Payment cần Admin đối soát (Spec 11.6, dòng 4, 6, 7). */
public enum ReviewReasonCode {
    ORDER_NOT_PAYABLE,
    AMOUNT_UNDERPAID,
    AMOUNT_OVERPAID
}
