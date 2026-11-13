package com.example.lms.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.example.lms.commerce.OrderStatus;
import com.example.lms.payment.PaymentDecision.Input;
import com.example.lms.payment.PaymentDecision.Outcome;

/** Mỗi dòng của bảng quyết định Spec 11.6 có test riêng, cộng các biên thời gian ân hạn. */
class PaymentDecisionTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-11-10T10:00:00Z");
    private static final Duration GRACE = Duration.ofMinutes(15);
    private static final Instant BEFORE_EXPIRY = EXPIRES_AT.minusSeconds(60);
    private static final long PRICE = 500_000;

    private static Input input(String reference, PaymentStatus payment, OrderStatus order, long received,
            Instant receivedAt) {
        return new Input(reference, payment, order, EXPIRES_AT, PRICE, received, receivedAt, GRACE);
    }

    private static Outcome decide(PaymentStatus payment, OrderStatus order, long received, Instant receivedAt) {
        return PaymentDecision.decide(input("LMSABCDEFGH12", payment, order, received, receivedAt));
    }

    @Test
    void row1_noReference_isUnmatched() {
        for (String reference : new String[] { null, "", "   " }) {
            Outcome outcome = PaymentDecision.decide(
                    input(reference, PaymentStatus.PENDING, OrderStatus.PENDING_PAYMENT, PRICE, BEFORE_EXPIRY));
            assertEquals(1, outcome.row());
            assertEquals(WebhookProcessingStatus.UNMATCHED, outcome.webhookStatus());
            assertEquals(UnmatchedReason.NO_REFERENCE, outcome.unmatchedReason());
            assertFalse(outcome.grantsAccess());
        }
    }

    @Test
    void row2_unknownReference_isUnmatched() {
        Outcome outcome = decide(null, null, PRICE, BEFORE_EXPIRY);
        assertEquals(2, outcome.row());
        assertEquals(UnmatchedReason.UNKNOWN_REFERENCE, outcome.unmatchedReason());
        assertNull(outcome.paymentStatusAfter());
    }

    @Test
    void row3_alreadyPaid_keepsPaymentPaid() {
        Outcome outcome = decide(PaymentStatus.PAID, OrderStatus.PAID, PRICE, BEFORE_EXPIRY);
        assertEquals(3, outcome.row());
        assertEquals(UnmatchedReason.ALREADY_PAID, outcome.unmatchedReason());
        assertEquals(PaymentStatus.PAID, outcome.paymentStatusAfter());
        assertFalse(outcome.grantsAccess());
    }

    @Test
    void row3_winsOverEveryLaterRow_evenWhenAmountIsWrong() {
        Outcome outcome = decide(PaymentStatus.PAID, OrderStatus.PAID, 1, EXPIRES_AT.plus(Duration.ofDays(3)));
        assertEquals(3, outcome.row());
    }

    @Test
    void row4_expiredOrder_movesPaymentToReview_acPay04() {
        Outcome outcome = decide(PaymentStatus.CANCELLED, OrderStatus.EXPIRED, PRICE, EXPIRES_AT.plusSeconds(3600));
        assertEquals(4, outcome.row());
        assertEquals(WebhookProcessingStatus.MATCHED, outcome.webhookStatus());
        assertEquals(PaymentStatus.REVIEW_REQUIRED, outcome.paymentStatusAfter());
        assertEquals(ReviewReasonCode.ORDER_NOT_PAYABLE, outcome.reviewReasonCode());
        assertFalse(outcome.grantsAccess());
        assertFalse(outcome.expireOrderFirst());
    }

    @Test
    void row4_cancelledOrder_movesPaymentToReview() {
        Outcome outcome = decide(PaymentStatus.CANCELLED, OrderStatus.CANCELLED, PRICE, BEFORE_EXPIRY);
        assertEquals(4, outcome.row());
        assertEquals(ReviewReasonCode.ORDER_NOT_PAYABLE, outcome.reviewReasonCode());
    }

    @Test
    void row4_takesPriorityOverAmountMismatch() {
        Outcome outcome = decide(PaymentStatus.CANCELLED, OrderStatus.EXPIRED, PRICE - 1, BEFORE_EXPIRY);
        assertEquals(4, outcome.row());
        assertEquals(ReviewReasonCode.ORDER_NOT_PAYABLE, outcome.reviewReasonCode());
    }

    @Test
    void row4_paymentAlreadyInReview_fallsToUnmatched_becauseReviewCannotReenterReview() {
        Outcome outcome = decide(PaymentStatus.REVIEW_REQUIRED, OrderStatus.EXPIRED, PRICE, BEFORE_EXPIRY);
        assertEquals(WebhookProcessingStatus.UNMATCHED, outcome.webhookStatus());
        assertEquals(UnmatchedReason.PAYMENT_NOT_PENDING, outcome.unmatchedReason());
        assertEquals(PaymentStatus.REVIEW_REQUIRED, outcome.paymentStatusAfter());
    }

    @Test
    void row4_failedPayment_fallsToUnmatched_becauseFailedIsFinal() {
        Outcome outcome = decide(PaymentStatus.FAILED, OrderStatus.EXPIRED, PRICE, BEFORE_EXPIRY);
        assertEquals(UnmatchedReason.PAYMENT_NOT_PENDING, outcome.unmatchedReason());
        assertEquals(PaymentStatus.FAILED, outcome.paymentStatusAfter());
    }

    @Test
    void row5_paymentInReviewWhileOrderPending_isUnmatched() {
        Outcome outcome = decide(PaymentStatus.REVIEW_REQUIRED, OrderStatus.PENDING_PAYMENT, PRICE, BEFORE_EXPIRY);
        assertEquals(5, outcome.row());
        assertEquals(UnmatchedReason.PAYMENT_NOT_PENDING, outcome.unmatchedReason());
        assertEquals(PaymentStatus.REVIEW_REQUIRED, outcome.paymentStatusAfter());
    }

    @Test
    void row5_paymentFailedWhileOrderPending_isUnmatched() {
        Outcome outcome = decide(PaymentStatus.FAILED, OrderStatus.PENDING_PAYMENT, PRICE, BEFORE_EXPIRY);
        assertEquals(5, outcome.row());
    }

    @Test
    void row6_underpaid_movesPaymentToReview_acPay03() {
        Outcome outcome = decide(PaymentStatus.PENDING, OrderStatus.PENDING_PAYMENT, 450_000, BEFORE_EXPIRY);
        assertEquals(6, outcome.row());
        assertEquals(WebhookProcessingStatus.MATCHED, outcome.webhookStatus());
        assertEquals(PaymentStatus.REVIEW_REQUIRED, outcome.paymentStatusAfter());
        assertEquals(ReviewReasonCode.AMOUNT_UNDERPAID, outcome.reviewReasonCode());
        assertFalse(outcome.grantsAccess());
        assertTrue(outcome.requiresReview());
    }

    @Test
    void row7_overpaid_movesPaymentToReview() {
        Outcome outcome = decide(PaymentStatus.PENDING, OrderStatus.PENDING_PAYMENT, PRICE + 1, BEFORE_EXPIRY);
        assertEquals(7, outcome.row());
        assertEquals(ReviewReasonCode.AMOUNT_OVERPAID, outcome.reviewReasonCode());
        assertFalse(outcome.grantsAccess());
    }

    @Test
    void row8_exactPaymentOnTime_paysAndGrantsAccess() {
        Outcome outcome = decide(PaymentStatus.PENDING, OrderStatus.PENDING_PAYMENT, PRICE, BEFORE_EXPIRY);
        assertEquals(8, outcome.row());
        assertEquals(WebhookProcessingStatus.PROCESSED, outcome.webhookStatus());
        assertEquals(PaymentStatus.PAID, outcome.paymentStatusAfter());
        assertTrue(outcome.grantsAccess());
        assertNull(outcome.unmatchedReason());
        assertNull(outcome.reviewReasonCode());
    }

    @Test
    void graceWindow_paymentAfterExpiryButWithinGrace_isProcessedNormally_acPay06() {
        Outcome outcome = decide(PaymentStatus.PENDING, OrderStatus.PENDING_PAYMENT, PRICE,
                EXPIRES_AT.plus(Duration.ofMinutes(14)));
        assertEquals(8, outcome.row());
        assertTrue(outcome.grantsAccess());
    }

    @Test
    void graceWindow_exactlyAtExpiryPlusGrace_isStillAccepted() {
        Outcome outcome = decide(PaymentStatus.PENDING, OrderStatus.PENDING_PAYMENT, PRICE, EXPIRES_AT.plus(GRACE));
        assertEquals(8, outcome.row());
    }

    @Test
    void lazyExpiry_afterGraceWhileJobHasNotRun_expiresOrderThenAppliesRow4() {
        Outcome outcome = decide(PaymentStatus.PENDING, OrderStatus.PENDING_PAYMENT, PRICE,
                EXPIRES_AT.plus(GRACE).plusMillis(1));
        assertEquals(4, outcome.row());
        assertTrue(outcome.expireOrderFirst());
        assertEquals(PaymentStatus.REVIEW_REQUIRED, outcome.paymentStatusAfter());
        assertEquals(ReviewReasonCode.ORDER_NOT_PAYABLE, outcome.reviewReasonCode());
        assertFalse(outcome.grantsAccess());
    }

    @Test
    void lazyExpiry_onlyAppliesToPendingPayment() {
        Outcome outcome = decide(PaymentStatus.REVIEW_REQUIRED, OrderStatus.PENDING_PAYMENT, PRICE,
                EXPIRES_AT.plus(Duration.ofDays(1)));
        assertEquals(5, outcome.row());
        assertFalse(outcome.expireOrderFirst());
    }

    @Test
    void everyOutcomeThatChangesPayment_isALegalTransition() {
        PaymentStatus[] payments = { PaymentStatus.PENDING, PaymentStatus.REVIEW_REQUIRED, PaymentStatus.FAILED,
                PaymentStatus.CANCELLED };
        Instant[] times = { BEFORE_EXPIRY, EXPIRES_AT.plus(Duration.ofHours(1)) };
        long[] amounts = { PRICE - 1, PRICE, PRICE + 1 };
        for (PaymentStatus payment : payments) {
            for (OrderStatus order : OrderStatus.values()) {
                for (Instant at : times) {
                    for (long amount : amounts) {
                        Outcome outcome = decide(payment, order, amount, at);
                        PaymentStatus before = outcome.expireOrderFirst() ? PaymentStatus.CANCELLED : payment;
                        if (outcome.paymentStatusAfter() != before) {
                            assertTrue(before.canTransitionTo(outcome.paymentStatusAfter()),
                                    payment + "/" + order + " produced illegal " + before + " -> "
                                            + outcome.paymentStatusAfter());
                        }
                    }
                }
            }
        }
    }
}
