package com.example.lms.commerce.dto;

import java.time.Instant;
import java.util.List;

import com.example.lms.commerce.Order;
import com.example.lms.commerce.OrderStatus;
import com.example.lms.payment.Payment;

/** OpenAPI {@code OrderDetail}. */
public record OrderDetailResponse(long id, String orderNumber, OrderStatus status, long subtotal, long discountTotal,
        long total, List<OrderItemResponse> items, PaymentStatusResponse payment, Instant createdAt,
        Instant expiresAt) {

    public static OrderDetailResponse from(Order order, Payment payment) {
        return new OrderDetailResponse(order.getId(), order.getCode(), order.getStatus(), order.getSubtotal(),
                order.getDiscountAmount(), order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                payment == null ? null : PaymentStatusResponse.from(order, payment), order.getCreatedAt(),
                order.getExpiresAt());
    }
}
