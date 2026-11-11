package com.example.lms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;

import com.example.lms.commerce.Order;
import com.example.lms.commerce.OrderStatus;
import com.example.lms.enrollment.Enrollment;
import com.example.lms.enrollment.EnrollmentSource;
import com.example.lms.enrollment.EnrollmentStatus;
import com.example.lms.exam.AttemptStatus;
import com.example.lms.exam.ExamAttempt;
import com.example.lms.exam.ExamStatus;
import com.example.lms.exam.SubmissionType;
import com.example.lms.payment.Payment;
import com.example.lms.payment.PaymentMethod;
import com.example.lms.payment.PaymentProviderType;
import com.example.lms.payment.PaymentStatus;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Spec Mục 24: mỗi máy trạng thái phải cho qua mọi cặp hợp lệ và từ chối mọi cặp còn lại.
 * Test duyệt toàn bộ tích (từ, đến) và so với danh sách cặp hợp lệ viết tay từ Spec.
 */
class StateMachineTest {

    private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");

    private record Pair<E>(E from, E to) {
    }

    private static <E extends Enum<E>> void assertExactly(E[] values, BiPredicate<E, E> rule, Set<Pair<E>> legal) {
        for (E from : values) {
            for (E to : values) {
                assertEquals(legal.contains(new Pair<>(from, to)), rule.test(from, to), from + " -> " + to);
            }
        }
    }

    @Test
    void orderTransitions_spec10_5() {
        assertExactly(OrderStatus.values(), OrderStatus::canTransitionTo, Set.of(
                new Pair<>(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID),
                new Pair<>(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED),
                new Pair<>(OrderStatus.PENDING_PAYMENT, OrderStatus.EXPIRED)));
    }

    @Test
    void orderTransitions_reconciliationAddsOnlyExpiredOrCancelledToPaid() {
        assertExactly(OrderStatus.values(), OrderStatus::canTransitionByReconciliation, Set.of(
                new Pair<>(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID),
                new Pair<>(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED),
                new Pair<>(OrderStatus.PENDING_PAYMENT, OrderStatus.EXPIRED),
                new Pair<>(OrderStatus.EXPIRED, OrderStatus.PAID),
                new Pair<>(OrderStatus.CANCELLED, OrderStatus.PAID)));
    }

    @Test
    void paymentTransitions_spec11_1() {
        assertExactly(PaymentStatus.values(), PaymentStatus::canTransitionTo, Set.of(
                new Pair<>(PaymentStatus.PENDING, PaymentStatus.PAID),
                new Pair<>(PaymentStatus.PENDING, PaymentStatus.REVIEW_REQUIRED),
                new Pair<>(PaymentStatus.PENDING, PaymentStatus.CANCELLED),
                new Pair<>(PaymentStatus.CANCELLED, PaymentStatus.REVIEW_REQUIRED),
                new Pair<>(PaymentStatus.REVIEW_REQUIRED, PaymentStatus.PAID),
                new Pair<>(PaymentStatus.REVIEW_REQUIRED, PaymentStatus.FAILED)));
    }

    @Test
    void enrollmentTransitions_spec12() {
        assertExactly(EnrollmentStatus.values(), EnrollmentStatus::canTransitionTo, Set.of(
                new Pair<>(EnrollmentStatus.ACTIVE, EnrollmentStatus.EXPIRED),
                new Pair<>(EnrollmentStatus.ACTIVE, EnrollmentStatus.REVOKED)));
    }

    @Test
    void attemptTransitions_spec14_3() {
        assertExactly(AttemptStatus.values(), AttemptStatus::canTransitionTo, Set.of(
                new Pair<>(AttemptStatus.IN_PROGRESS, AttemptStatus.SUBMITTED),
                new Pair<>(AttemptStatus.IN_PROGRESS, AttemptStatus.GRADED),
                new Pair<>(AttemptStatus.SUBMITTED, AttemptStatus.GRADED)));
    }

    @Test
    void examTransitions() {
        assertExactly(ExamStatus.values(), ExamStatus::canTransitionTo, Set.of(
                new Pair<>(ExamStatus.DRAFT, ExamStatus.PUBLISHED),
                new Pair<>(ExamStatus.PUBLISHED, ExamStatus.CLOSED)));
    }

    // Các entity thực thi máy trạng thái của chính mình

    private static Order newOrder() {
        return new Order("ORD1", 1, null, null, 500_000, 0, "key", "hash", NOW.plusSeconds(3600), NOW);
    }

    @Test
    void order_paidIsTerminal_andExpiredNeedsReconciliationToBePaid() {
        Order paid = newOrder();
        paid.markPaid(NOW);
        assertEquals(ErrorCode.STATE_CONFLICT, assertThrows(BusinessException.class, () -> paid.cancel(NOW)).code());

        Order expired = newOrder();
        expired.expire(NOW);
        assertThrows(BusinessException.class, () -> expired.markPaid(NOW));
        expired.markPaidByReconciliation(NOW);
        assertEquals(OrderStatus.PAID, expired.getStatus());
    }

    @Test
    void order_totalIsSubtotalMinusDiscount() {
        Order order = new Order("ORD2", 1, 3L, "SALE", 500_000, 50_000, "k", "h", NOW, NOW);
        assertEquals(450_000, order.getTotalAmount());
        assertThrows(IllegalArgumentException.class,
                () -> new Order("ORD3", 1, null, null, 100, 101, "k", "h", NOW, NOW));
    }

    @Test
    void order_acceptsAutomaticPaymentUntilExpiryPlusGrace() {
        Order order = newOrder();
        java.time.Duration grace = java.time.Duration.ofMinutes(15);
        Instant deadline = order.getExpiresAt().plus(grace);
        assertEquals(true, order.acceptsAutomaticPaymentAt(deadline, grace));
        assertEquals(false, order.acceptsAutomaticPaymentAt(deadline.plusMillis(1), grace));
    }

    @Test
    void payment_reviewThenApprove_keepsTransactionBinding() {
        Payment payment = Payment.pending(1, PaymentProviderType.SEPAY, PaymentMethod.BANK_QR, 500_000,
                "LMSABCDEFGH12", null, NOW);
        payment.markReviewRequired(com.example.lms.payment.ReviewReasonCode.AMOUNT_UNDERPAID, "underpaid",
                450_000, 10, "SEPAY-1", NOW);
        assertThrows(BusinessException.class, () -> payment.cancel(NOW));
        assertThrows(BusinessException.class, () -> payment.markPaid(450_000, 11, "SEPAY-2", NOW));
        payment.markPaid(450_000, 10, "SEPAY-1", NOW);
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(450_000L, payment.getReceivedAmount());
        assertThrows(BusinessException.class, () -> payment.markFailed("late", NOW));
    }

    @Test
    void enrollment_revokeNeedsReason_andOnlyFromActive() {
        Enrollment enrollment = Enrollment.grant(1, 2, null, EnrollmentSource.ADMIN_GRANTED, NOW, null, NOW);
        assertThrows(IllegalArgumentException.class, () -> enrollment.revoke(9, " ", NOW));
        enrollment.revoke(9, "refund", NOW);
        assertEquals(EnrollmentStatus.REVOKED, enrollment.getStatus());
        assertThrows(BusinessException.class, () -> enrollment.expire(NOW));
    }

    @Test
    void attempt_expiresExactlyAtExpiresAt_andAutoExpiredSubmitsAtDeadline() {
        Instant expiresAt = NOW.plusSeconds(1800);
        ExamAttempt attempt = ExamAttempt.start(1, 2, 3, 1, NOW, expiresAt, BigDecimal.TEN, BigDecimal.valueOf(50));
        assertEquals(false, attempt.isExpiredAt(expiresAt.minusMillis(1)));
        assertEquals(true, attempt.isExpiredAt(expiresAt));

        Instant jobRanLate = expiresAt.plusSeconds(45);
        attempt.grade(SubmissionType.AUTO_EXPIRED, BigDecimal.ZERO, false, jobRanLate);
        assertEquals(AttemptStatus.GRADED, attempt.getStatus());
        assertEquals(expiresAt, attempt.getSubmittedAt());
        assertEquals(jobRanLate, attempt.getGradedAt());
        assertThrows(BusinessException.class,
                () -> attempt.grade(SubmissionType.MANUAL, BigDecimal.TEN, true, jobRanLate));
    }
}
