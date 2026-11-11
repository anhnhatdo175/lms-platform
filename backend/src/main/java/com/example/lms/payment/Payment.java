package com.example.lms.payment;

import java.time.Instant;
import java.util.Objects;

import com.example.lms.shared.exception.BusinessException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Payment của một Order. Mọi đổi trạng thái đi qua {@link #transitionTo} để giữ đúng 6 cặp ở Spec 11.1. */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PaymentProviderType provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PaymentMethod method;

    @Column(name = "expected_amount", nullable = false, updatable = false)
    private long expectedAmount;

    @Column(name = "received_amount")
    private Long receivedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_reference", nullable = false, updatable = false, length = 100)
    private String paymentReference;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @Column(name = "qr_content", columnDefinition = "TEXT")
    private String qrContent;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_reason_code")
    private ReviewReasonCode reviewReasonCode;

    @Column(name = "review_reason", length = 500)
    private String reviewReason;

    /** Webhook đã chốt thanh toán, hoặc webhook gây review. */
    @Column(name = "paid_webhook_log_id")
    private Long paidWebhookLogId;

    @Column(name = "failed_reason", length = 500)
    private String failedReason;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public static Payment pending(long orderId, PaymentProviderType provider, PaymentMethod method,
            long expectedAmount, String paymentReference, String qrContent, Instant now) {
        Payment payment = new Payment();
        payment.orderId = orderId;
        payment.provider = provider;
        payment.method = method;
        payment.expectedAmount = expectedAmount;
        payment.paymentReference = paymentReference;
        payment.qrContent = qrContent;
        payment.status = PaymentStatus.PENDING;
        payment.createdAt = now;
        payment.updatedAt = now;
        return payment;
    }

    /**
     * Ghi nhận đã thu tiền: dòng 8 của bảng quyết định, hoặc Admin approve từ REVIEW_REQUIRED.
     * {@code receivedAmount} là số thực nhận; doanh thu tính theo số này (Spec 15.1).
     */
    public void markPaid(long receivedAmount, long webhookLogId, String providerTransactionId, Instant now) {
        requireTransition(PaymentStatus.PAID);
        requireCompatibleTransaction(providerTransactionId);
        status = PaymentStatus.PAID;
        bindTransaction(providerTransactionId);
        this.receivedAmount = receivedAmount;
        this.paidWebhookLogId = webhookLogId;
        this.paidAt = now;
        this.updatedAt = now;
    }

    /** Dòng 4, 6, 7: ghi sớm mã giao dịch để UNIQUE chặn giao dịch này bị gắn vào Payment khác. */
    public void markReviewRequired(ReviewReasonCode reasonCode, String reason, long receivedAmount,
            long webhookLogId, String providerTransactionId, Instant now) {
        requireTransition(PaymentStatus.REVIEW_REQUIRED);
        requireCompatibleTransaction(providerTransactionId);
        status = PaymentStatus.REVIEW_REQUIRED;
        bindTransaction(providerTransactionId);
        this.reviewReasonCode = reasonCode;
        this.reviewReason = reason;
        this.receivedAmount = receivedAmount;
        this.paidWebhookLogId = webhookLogId;
        this.updatedAt = now;
    }

    /** Student hủy Order hoặc Order hết hạn. */
    public void cancel(Instant now) {
        transitionTo(PaymentStatus.CANCELLED);
        this.updatedAt = now;
    }

    /** Admin reject đối soát. */
    public void markFailed(String reason, Instant now) {
        transitionTo(PaymentStatus.FAILED);
        this.failedReason = reason;
        this.updatedAt = now;
    }

    // Kiểm tra mọi điều kiện trước rồi mới đổi field, để lỗi giữa chừng không để entity ở trạng thái dở dang.

    private void transitionTo(PaymentStatus target) {
        requireTransition(target);
        status = target;
    }

    private void requireTransition(PaymentStatus target) {
        if (!status.canTransitionTo(target)) {
            throw BusinessException.stateConflict("Payment " + id + " cannot move from " + status + " to " + target);
        }
    }

    private void requireCompatibleTransaction(String transactionId) {
        if (transactionId != null && providerTransactionId != null
                && !Objects.equals(providerTransactionId, transactionId)) {
            throw BusinessException.stateConflict("Payment " + id + " is already bound to another transaction");
        }
    }

    private void bindTransaction(String transactionId) {
        if (transactionId != null) {
            providerTransactionId = transactionId;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public PaymentProviderType getProvider() {
        return provider;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public long getExpectedAmount() {
        return expectedAmount;
    }

    public Long getReceivedAmount() {
        return receivedAmount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public String getQrContent() {
        return qrContent;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public ReviewReasonCode getReviewReasonCode() {
        return reviewReasonCode;
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public Long getPaidWebhookLogId() {
        return paidWebhookLogId;
    }

    public String getFailedReason() {
        return failedReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
