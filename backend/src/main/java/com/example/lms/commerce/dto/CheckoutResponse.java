package com.example.lms.commerce.dto;

import java.time.Instant;

import com.example.lms.commerce.Order;
import com.example.lms.payment.Payment;
import com.example.lms.payment.PaymentStatus;

/** OpenAPI {@code CheckoutResult}: Order đã snapshot và Payment PENDING kèm QR. */
public record CheckoutResponse(OrderDetailResponse order, CheckoutPayment payment) {

    public record CheckoutPayment(long id, String provider, PaymentStatus status, String paymentReference, long amount,
            String instructions, String qrCodeUrl, Instant expiresAt) {
    }

    public static CheckoutResponse from(Order order, Payment payment) {
        String instructions = "Chuyển khoản đúng " + payment.getExpectedAmount() + " đ với nội dung "
                + payment.getPaymentReference() + " trước khi đơn hết hạn.";
        return new CheckoutResponse(OrderDetailResponse.from(order, payment),
                new CheckoutPayment(payment.getId(), payment.getProvider().name(), payment.getStatus(),
                        payment.getPaymentReference(), payment.getExpectedAmount(), instructions,
                        payment.getQrContent(), order.getExpiresAt()));
    }
}
