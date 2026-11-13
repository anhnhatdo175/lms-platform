package com.example.lms.commerce.dto;

import jakarta.validation.constraints.Pattern;

/** OpenAPI {@code CheckoutRequest}; header {@code Idempotency-Key} bắt buộc, đọc ở controller. */
public record CheckoutRequest(String couponCode,
        @Pattern(regexp = "SEPAY_BANK_TRANSFER") String paymentMethod) {
}
