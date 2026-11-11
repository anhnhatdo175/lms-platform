package com.example.lms.commerce.dto;

import java.time.Instant;

import com.example.lms.commerce.Order;
import com.example.lms.commerce.OrderStatus;

/** OpenAPI {@code OrderSummary}. */
public record OrderSummaryResponse(long id, String orderNumber, OrderStatus status, long total, int itemCount,
        Instant createdAt, Instant expiresAt) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getCode(), order.getStatus(), order.getTotalAmount(),
                order.getItems().size(), order.getCreatedAt(), order.getExpiresAt());
    }
}
