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

/** Lịch sử thao tác đối soát của Admin; mọi thao tác bắt buộc có lý do (Spec 11.6). Chỉ ghi thêm, không sửa. */
@Entity
@Table(name = "payment_reconciliation_actions")
public class PaymentReconciliationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "webhook_log_id", nullable = false, updatable = false)
    private Long webhookLogId;

    @Column(name = "payment_id", updatable = false)
    private Long paymentId;

    @Column(name = "order_id", updatable = false)
    private Long orderId;

    @Column(name = "performed_by", nullable = false, updatable = false)
    private Long performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, updatable = false)
    private ReconciliationActionType actionType;

    @Column(name = "previous_status", updatable = false, length = 50)
    private String previousStatus;

    @Column(name = "new_status", updatable = false, length = 50)
    private String newStatus;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentReconciliationAction() {
    }

    public PaymentReconciliationAction(long webhookLogId, Long paymentId, Long orderId, long performedBy,
            ReconciliationActionType actionType, String previousStatus, String newStatus, String note,
            Instant now) {
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("A reconciliation action requires a reason");
        }
        this.webhookLogId = webhookLogId;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.performedBy = performedBy;
        this.actionType = actionType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.note = note;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getWebhookLogId() {
        return webhookLogId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getPerformedBy() {
        return performedBy;
    }

    public ReconciliationActionType getActionType() {
        return actionType;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
