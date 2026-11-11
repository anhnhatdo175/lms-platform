package com.example.lms.commerce.dto;

import java.time.Instant;

import com.example.lms.commerce.Order;
import com.example.lms.commerce.OrderStatus;
import com.example.lms.payment.Payment;
import com.example.lms.payment.PaymentStatus;
import com.example.lms.payment.ReviewReasonCode;

/**
 * OpenAPI {@code PaymentStatus}, dùng cho cả {@code /orders/{id}/payment} và {@code /orders/{id}/status}
 * (Student poll 3 giây). {@code checkoutUrl} là link ảnh QR.
 */
public record PaymentStatusResponse(long orderId, String orderNumber, OrderStatus orderStatus,
        PaymentStatus paymentStatus, String provider, String paymentReference, long amount, String checkoutUrl,
        Instant paidAt, Instant updatedAt, Long receivedAmount, String reviewReason,
        ReviewReasonCode reviewReasonCode) {

    public static PaymentStatusResponse from(Order order, Payment payment) {
        boolean inReview = payment.getStatus() == PaymentStatus.REVIEW_REQUIRED;
        boolean showsReceived = inReview || payment.getStatus() == PaymentStatus.PAID;
        Instant updatedAt = order.getUpdatedAt().isAfter(payment.getUpdatedAt()) ? order.getUpdatedAt()
                : payment.getUpdatedAt();
        return new PaymentStatusResponse(order.getId(), order.getCode(), order.getStatus(), payment.getStatus(),
                payment.getProvider().name(), payment.getPaymentReference(), payment.getExpectedAmount(),
                payment.getQrContent(), payment.getPaidAt(), updatedAt,
                showsReceived ? payment.getReceivedAmount() : null, inReview ? payment.getReviewReason() : null,
                inReview ? payment.getReviewReasonCode() : null);
    }
}
