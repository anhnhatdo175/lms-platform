package com.example.lms.commerce.dto;

import java.time.Instant;
import java.util.List;

/** OpenAPI {@code Cart}. Giá là giá bán hiện tại; Cart không lưu giá. */
public record CartResponse(List<CartItemResponse> items, long subtotal, int itemCount, Instant updatedAt) {

    public record CartItemResponse(long courseId, String courseSlug, String courseTitle, long price,
            String thumbnailUrl) {
    }

    public static CartResponse empty() {
        return new CartResponse(List.of(), 0, 0, null);
    }
}
