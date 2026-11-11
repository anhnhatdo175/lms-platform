package com.example.lms.payment;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Raw webhook, lưu trước transaction nghiệp vụ (Spec 11.7). Chỉ lần nhận đầu đã xác thực mới giữ
 * {@code primary_dedup_key}; webhook trùng và webhook sai key để NULL.
 */
@Entity
@Table(name = "payment_webhook_logs")
public class PaymentWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id")
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PaymentProviderType provider;

    @Column(name = "provider_event_id", updatable = false)
    private String providerEventId;

    @Column(name = "provider_transaction_id", updatable = false)
    private String providerTransactionId;

    @Column(name = "payment_reference", updatable = false, length = 100)
    private String paymentReference;

    @Column(name = "received_amount", updatable = false)
    private Long receivedAmount;

    @Column(nullable = false, updatable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "dedup_key", nullable = false, updatable = false, length = 300)
    private String dedupKey;

    @Column(name = "primary_dedup_key", updatable = false, length = 300)
    private String primaryDedupKey;

    @Column(nullable = false, updatable = false)
    private boolean authenticated;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private WebhookProcessingStatus processingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "unmatched_reason")
    private UnmatchedReason unmatchedReason;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected PaymentWebhookLog() {
    }

    /** Lần nhận đầu đã xác thực: giữ {@code primary_dedup_key}; INSERT vi phạm UNIQUE nghĩa là đã có bản đầu. */
    public static PaymentWebhookLog firstReceipt(PaymentProviderType provider, String providerEventId,
            String providerTransactionId, String paymentReference, Long receivedAmount, String payload,
            String dedupKey, Instant receivedAt) {
        PaymentWebhookLog log = base(provider, providerEventId, providerTransactionId, paymentReference,
                receivedAmount, payload, dedupKey, receivedAt);
        log.authenticated = true;
        log.primaryDedupKey = dedupKey;
        log.processingStatus = WebhookProcessingStatus.RECEIVED;
        return log;
    }

    /** Bản lặp: ghi để có dấu vết đầy đủ, không xử lý nghiệp vụ (Spec 11.4). */
    public static PaymentWebhookLog duplicateOf(PaymentProviderType provider, String providerEventId,
            String providerTransactionId, String paymentReference, Long receivedAmount, String payload,
            String dedupKey, Instant receivedAt) {
        PaymentWebhookLog log = base(provider, providerEventId, providerTransactionId, paymentReference,
                receivedAmount, payload, dedupKey, receivedAt);
        log.authenticated = true;
        log.processingStatus = WebhookProcessingStatus.DUPLICATE;
        log.processedAt = receivedAt;
        return log;
    }

    /**
     * Webhook sai API key. Không giữ {@code primary_dedup_key}: nếu giữ, webhook giả gửi trước một {@code id}
     * sẽ làm webhook thật đến sau bị coi là trùng và khoản tiền mất dấu.
     */
    public static PaymentWebhookLog unauthenticated(PaymentProviderType provider, String payload, String dedupKey,
            Instant receivedAt) {
        PaymentWebhookLog log = base(provider, null, null, null, null, payload, dedupKey, receivedAt);
        log.authenticated = false;
        log.processingStatus = WebhookProcessingStatus.FAILED;
        log.errorMessage = "Invalid API key";
        log.processedAt = receivedAt;
        return log;
    }

    /** Đã xác thực nhưng không phải tiền vào tài khoản của hệ thống (tiền ra, sai số tài khoản, thiếu id). */
    public static PaymentWebhookLog ignored(PaymentProviderType provider, String providerTransactionId,
            String paymentReference, Long receivedAmount, String payload, String dedupKey, String reason,
            Instant receivedAt) {
        PaymentWebhookLog log = base(provider, null, providerTransactionId, paymentReference, receivedAmount,
                payload, dedupKey, receivedAt);
        log.authenticated = true;
        log.processingStatus = WebhookProcessingStatus.FAILED;
        log.errorMessage = reason;
        log.processedAt = receivedAt;
        return log;
    }

    private static PaymentWebhookLog base(PaymentProviderType provider, String providerEventId,
            String providerTransactionId, String paymentReference, Long receivedAmount, String payload,
            String dedupKey, Instant receivedAt) {
        PaymentWebhookLog log = new PaymentWebhookLog();
        log.provider = provider;
        log.providerEventId = providerEventId;
        log.providerTransactionId = providerTransactionId;
        log.paymentReference = paymentReference;
        log.receivedAmount = receivedAmount;
        log.payload = payload;
        log.dedupKey = dedupKey;
        log.receivedAt = receivedAt;
        return log;
    }

    public void markUnmatched(UnmatchedReason reason, Long paymentId, Instant now) {
        this.processingStatus = WebhookProcessingStatus.UNMATCHED;
        this.unmatchedReason = reason;
        this.paymentId = paymentId;
        this.processedAt = now;
    }

    /** Dòng 4, 6, 7: đã gắn được Payment nhưng Payment chuyển REVIEW_REQUIRED. */
    public void markMatchedForReview(long paymentId, Instant now) {
        this.processingStatus = WebhookProcessingStatus.MATCHED;
        this.unmatchedReason = null;
        this.paymentId = paymentId;
        this.processedAt = now;
    }

    public void markProcessed(long paymentId, Instant now) {
        this.processingStatus = WebhookProcessingStatus.PROCESSED;
        this.unmatchedReason = null;
        this.paymentId = paymentId;
        this.processedAt = now;
    }

    public void markFailed(String message, Instant now) {
        this.processingStatus = WebhookProcessingStatus.FAILED;
        this.errorMessage = message;
        this.processedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public PaymentProviderType getProvider() {
        return provider;
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public Long getReceivedAmount() {
        return receivedAmount;
    }

    public String getPayload() {
        return payload;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public String getPrimaryDedupKey() {
        return primaryDedupKey;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public WebhookProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public UnmatchedReason getUnmatchedReason() {
        return unmatchedReason;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
