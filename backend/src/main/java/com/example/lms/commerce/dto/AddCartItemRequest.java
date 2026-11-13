package com.example.lms.commerce.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** OpenAPI {@code CartItemRequest}. */
public record AddCartItemRequest(@NotNull @Positive Long courseId) {
}
