package com.example.lms.commerce.dto;

import com.example.lms.commerce.OrderItem;

/** OpenAPI {@code OrderItem}; {@code unitPrice} là giá bán snapshot lúc mua. */
public record OrderItemResponse(long id, long courseId, String courseTitle, long unitPrice, long discountAmount,
        long finalPrice) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getId(), item.getCourseId(), item.getCourseTitle(), item.getOriginalPrice(),
                item.getDiscountAmount(), item.getFinalPrice());
    }
}
