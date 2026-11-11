package com.example.lms.payment;

import java.time.Duration;
import java.time.Instant;

import com.example.lms.commerce.OrderStatus;

/**
 * Bảng quyết định đối soát (Spec 11.6, DEC-005), viết thành hàm thuần: không đọc database, không đổi trạng thái.
 * Service khóa Order → Payment, gọi {@link #decide}, rồi áp {@link Outcome} trong cùng transaction.
 * Các dòng xét đúng thứ tự từ trên xuống; dòng đầu tiên khớp thì dừng.
 */
public final class PaymentDecision {

    private PaymentDecision() {
    }

    /**
     * @param paymentReference mã thanh toán đọc được từ webhook; {@code null} hoặc rỗng nếu không có
     * @param paymentStatus    trạng thái Payment khớp reference; {@code null} nếu không tìm thấy Payment
     * @param orderStatus      trạng thái Order của Payment đó
     * @param orderExpiresAt   {@code orders.expires_at}
     * @param receivedAt       lúc server nhận webhook (không dùng {@code transactionDate} của ngân hàng)
     */
    public record Input(String paymentReference, PaymentStatus paymentStatus, OrderStatus orderStatus,
            Instant orderExpiresAt, long expectedAmount, long receivedAmount, Instant receivedAt,
            Duration lateGrace) {
    }

    /**
     * @param row               dòng của bảng đã khớp (1–8)
     * @param paymentStatusAfter trạng thái Payment sau khi áp; bằng trạng thái cũ nếu không đổi, {@code null} nếu không có Payment
     * @param expireOrderFirst  Order đã quá {@code expiresAt + grace} mà job chưa kịp chạy: chuyển Order EXPIRED
     *                          và Payment CANCELLED trước, rồi mới áp dòng 4
     */
    public record Outcome(int row, WebhookProcessingStatus webhookStatus, PaymentStatus paymentStatusAfter,
            UnmatchedReason unmatchedReason, ReviewReasonCode reviewReasonCode, boolean expireOrderFirst) {

        /** Chỉ dòng 8 cấp quyền học tự động (Spec 11.9). */
        public boolean grantsAccess() {
            return row == 8;
        }

        public boolean requiresReview() {
            return paymentStatusAfter == PaymentStatus.REVIEW_REQUIRED && reviewReasonCode != null;
        }
    }

    public static Outcome decide(Input in) {
        // Dòng 1
        if (in.paymentReference() == null || in.paymentReference().isBlank()) {
            return unmatched(1, UnmatchedReason.NO_REFERENCE, null);
        }
        // Dòng 2: reference sai định dạng cũng không tìm thấy Payment
        if (in.paymentStatus() == null) {
            return unmatched(2, UnmatchedReason.UNKNOWN_REFERENCE, null);
        }
        // Dòng 3: khách chuyển lần hai cho đơn đã trả
        if (in.paymentStatus() == PaymentStatus.PAID) {
            return unmatched(3, UnmatchedReason.ALREADY_PAID, PaymentStatus.PAID);
        }

        // Hết hạn lười: kết quả không phụ thuộc job hết hạn đã chạy hay chưa (Spec 10.6)
        boolean lapsed = in.orderStatus() == OrderStatus.PENDING_PAYMENT
                && in.paymentStatus() == PaymentStatus.PENDING
                && in.receivedAt().isAfter(in.orderExpiresAt().plus(in.lateGrace()));
        OrderStatus order = lapsed ? OrderStatus.EXPIRED : in.orderStatus();
        PaymentStatus payment = lapsed ? PaymentStatus.CANCELLED : in.paymentStatus();

        // Dòng 4: tiền đến khi Order không còn nhận thanh toán tự động
        if (order != OrderStatus.PENDING_PAYMENT) {
            if (payment == PaymentStatus.PENDING || payment == PaymentStatus.CANCELLED) {
                return new Outcome(4, WebhookProcessingStatus.MATCHED, PaymentStatus.REVIEW_REQUIRED, null,
                        ReviewReasonCode.ORDER_NOT_PAYABLE, lapsed);
            }
            // Payment đã REVIEW_REQUIRED (từ một webhook trước) hoặc FAILED: không thể vào REVIEW_REQUIRED lần nữa
            // theo Spec 11.1, nên giao dịch này vào hàng đợi UNMATCHED như dòng 5. Spec 11.6 chưa nói ca này.
            return unmatched(5, UnmatchedReason.PAYMENT_NOT_PENDING, payment);
        }
        // Dòng 5
        if (payment != PaymentStatus.PENDING) {
            return unmatched(5, UnmatchedReason.PAYMENT_NOT_PENDING, payment);
        }
        // Dòng 6, 7: MVP không cộng dồn nhiều giao dịch cho một Payment (Spec 11.5)
        if (in.receivedAmount() < in.expectedAmount()) {
            return new Outcome(6, WebhookProcessingStatus.MATCHED, PaymentStatus.REVIEW_REQUIRED, null,
                    ReviewReasonCode.AMOUNT_UNDERPAID, false);
        }
        if (in.receivedAmount() > in.expectedAmount()) {
            return new Outcome(7, WebhookProcessingStatus.MATCHED, PaymentStatus.REVIEW_REQUIRED, null,
                    ReviewReasonCode.AMOUNT_OVERPAID, false);
        }
        // Dòng 8: điều kiện duy nhất để cấp quyền học tự động
        return new Outcome(8, WebhookProcessingStatus.PROCESSED, PaymentStatus.PAID, null, null, false);
    }

    private static Outcome unmatched(int row, UnmatchedReason reason, PaymentStatus unchanged) {
        return new Outcome(row, WebhookProcessingStatus.UNMATCHED, unchanged, reason, null, false);
    }
}
