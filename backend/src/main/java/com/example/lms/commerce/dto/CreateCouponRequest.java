package com.example.lms.commerce.dto;

import java.time.Instant;
import java.util.List;

import com.example.lms.commerce.CouponScope;
import com.example.lms.commerce.DiscountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** OpenAPI {@code CreateCouponRequest}; {@code courseIds} bắt buộc khi {@code scope = SPECIFIC_COURSES}. */
public record CreateCouponRequest(@NotBlank @Size(min = 3, max = 50) String code, @NotBlank String name,
        @NotNull DiscountType discountType, @NotNull @Positive Long discountValue,
        @PositiveOrZero Long minimumOrderAmount, @PositiveOrZero Long maximumDiscountAmount,
        @Positive Integer totalUsageLimit, @Positive Integer perUserLimit, @NotNull CouponScope scope,
        List<@NotNull @Positive Long> courseIds, @NotNull Instant startsAt, @NotNull Instant endsAt) {
}
