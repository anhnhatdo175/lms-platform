package com.example.lms.commerce;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc Coupon kèm phạm vi Course và số lần User đã dùng, cho preview và checkout. */
@Service
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository coupons;
    private final CouponCourseRepository couponCourses;
    private final CouponRedemptionRepository redemptions;

    public CouponService(CouponRepository coupons, CouponCourseRepository couponCourses,
            CouponRedemptionRepository redemptions) {
        this.coupons = coupons;
        this.couponCourses = couponCourses;
        this.redemptions = redemptions;
    }

    public record LoadedCoupon(Coupon coupon, CouponRule rule, long userRedemptions) {
    }

    public Optional<LoadedCoupon> load(String code, long userId) {
        return coupons.findByCode(Coupon.normalizeCode(code)).map(coupon -> {
            Set<Long> courseIds = couponCourses.findByCouponId(coupon.getId()).stream()
                    .map(CouponCourse::getCourseId).collect(Collectors.toSet());
            return new LoadedCoupon(coupon, coupon.toRule(courseIds),
                    redemptions.countByCouponIdAndUserId(coupon.getId(), userId));
        });
    }
}
