package com.example.lms.commerce.dto;

import jakarta.validation.constraints.NotBlank;

public record CouponPreviewRequest(@NotBlank String couponCode) {
}
