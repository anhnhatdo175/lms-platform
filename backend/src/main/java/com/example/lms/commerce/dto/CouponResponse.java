package com.example.lms.commerce.dto;

import java.time.Instant;
import java.util.List;

import com.example.lms.commerce.Coupon;
import com.example.lms.commerce.CouponScope;
import com.example.lms.commerce.CouponStatus;
import com.example.lms.commerce.DiscountType;

/** OpenAPI {@code Coupon}; {@code usedCount} chỉ tăng khi Payment thành công. */
public record CouponResponse(long id, String code, String name, DiscountType discountType, long discountValue,
        long minimumOrderAmount, Long maximumDiscountAmount, Integer totalUsageLimit, Integer perUserLimit,
        CouponScope scope, List<Long> courseIds, Instant startsAt, Instant endsAt, int usedCount,
        CouponStatus status, Instant createdAt, Instant updatedAt) {

    public static CouponResponse from(Coupon coupon, List<Long> courseIds) {
        return new CouponResponse(coupon.getId(), coupon.getCode(), coupon.getName(), coupon.getDiscountType(),
                coupon.getDiscountValue(), coupon.getMinimumOrderAmount(), coupon.getMaximumDiscountAmount(),
                coupon.getTotalUsageLimit(), coupon.getPerUserLimit(), coupon.getScope(), courseIds,
                coupon.getStartsAt(), coupon.getEndsAt(), coupon.getUsedCount(), coupon.getStatus(),
                coupon.getCreatedAt(), coupon.getUpdatedAt());
    }
}
