package com.example.lms.commerce.dto;

import com.example.lms.shared.exception.ErrorCode;

/** OpenAPI {@code CouponPreview}: chỉ là gợi ý, checkout kiểm tra lại và không giữ chỗ lượt dùng. */
public record CouponPreviewResponse(boolean valid, String couponCode, Long subtotal, Long discountTotal, Long total,
        ErrorCode reasonCode) {
}
