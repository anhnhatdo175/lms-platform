package com.example.lms.payment.dto;

import java.time.Instant;

import com.example.lms.payment.Payment;
import com.example.lms.payment.PaymentReconciliationAction;
import com.example.lms.payment.PaymentStatus;
import com.example.lms.payment.PaymentWebhookLog;
import com.example.lms.payment.ReconciliationActionType;
import com.example.lms.payment.ReviewReasonCode;
import com.example.lms.payment.UnmatchedReason;
import com.example.lms.payment.WebhookProcessingStatus;

/** OpenAPI {@code ReconciliationItem}: một webhook, kèm Payment liên quan và thao tác Admin gần nhất. */
public record ReconciliationItemResponse(long id, String provider, String providerTransactionId, long amount,
        WebhookProcessingStatus status, Long orderId, Instant receivedAt, String reviewNote, Long paymentId,
        String paymentReference, PaymentStatus paymentStatus, Long expectedAmount, String reviewReason,
        ReconciliationActionType lastAction, UnmatchedReason unmatchedReason, ReviewReasonCode reviewReasonCode) {

    public static ReconciliationItemResponse from(PaymentWebhookLog log, Payment payment,
            PaymentReconciliationAction lastAction) {
        return new ReconciliationItemResponse(log.getId(), log.getProvider().name(), log.getProviderTransactionId(),
                log.getReceivedAmount() == null ? 0 : log.getReceivedAmount(), log.getProcessingStatus(),
                payment == null ? null : payment.getOrderId(), log.getReceivedAt(),
                lastAction == null ? null : lastAction.getNote(), payment == null ? null : payment.getId(),
                log.getPaymentReference(), payment == null ? null : payment.getStatus(),
                payment == null ? null : payment.getExpectedAmount(), payment == null ? null : payment.getReviewReason(),
                lastAction == null ? null : lastAction.getActionType(), log.getUnmatchedReason(),
                payment == null ? null : payment.getReviewReasonCode());
    }
}
