package com.example.lms.commerce.dto;

import java.time.Instant;

import com.example.lms.commerce.CouponStatus;

import jakarta.validation.constraints.Positive;

/** OpenAPI {@code UpdateCouponRequest}: không đổi mã, loại, giá trị; trường bỏ trống giữ nguyên. */
public record UpdateCouponRequest(String name, CouponStatus status, Instant endsAt, @Positive Integer totalUsageLimit,
        @Positive Integer perUserLimit) {
}
