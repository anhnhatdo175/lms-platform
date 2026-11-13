package com.example.lms.commerce;

import java.time.Instant;
import java.util.Set;

/** Ảnh chụp bất biến của một Coupon để tính giá; {@code courseIds} chỉ có nghĩa khi scope là SPECIFIC_COURSES. */
public record CouponRule(String code, DiscountType discountType, long discountValue, long minimumOrderAmount,
        Long maximumDiscountAmount, Integer totalUsageLimit, Integer perUserLimit, int usedCount, CouponScope scope,
        Set<Long> courseIds, Instant startsAt, Instant endsAt, CouponStatus status) {

    public CouponRule {
        courseIds = courseIds == null ? Set.of() : Set.copyOf(courseIds);
    }

    public boolean appliesTo(long courseId) {
        return scope == CouponScope.ALL_COURSES || courseIds.contains(courseId);
    }
}
