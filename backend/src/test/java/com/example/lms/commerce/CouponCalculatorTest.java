package com.example.lms.commerce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.lms.commerce.CouponCalculator.Line;
import com.example.lms.commerce.CouponCalculator.Pricing;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

class CouponCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");
    private static final Instant START = NOW.minusSeconds(86_400);
    private static final Instant END = NOW.plusSeconds(86_400);

    private static CouponRule percent(long value, Long cap) {
        return rule(DiscountType.PERCENTAGE, value, 0, cap, CouponScope.ALL_COURSES, Set.of());
    }

    private static CouponRule rule(DiscountType type, long value, long minimum, Long cap, CouponScope scope,
            Set<Long> courses) {
        return new CouponRule("SALE", type, value, minimum, cap, null, null, 0, scope, courses, START, END,
                CouponStatus.ACTIVE);
    }

    private static long sumOfItems(Pricing pricing) {
        return pricing.discountByCourse().values().stream().mapToLong(Long::longValue).sum();
    }

    @Test
    void withoutCoupon_totalEqualsSubtotal() {
        Pricing pricing = CouponCalculator.withoutCoupon(List.of(new Line(1, 300_000), new Line(2, 200_000)));
        assertEquals(500_000, pricing.subtotal());
        assertEquals(0, pricing.discountTotal());
        assertEquals(500_000, pricing.total());
        assertEquals(0, pricing.discountFor(1));
    }

    @Test
    void percentage_roundsDown() {
        Pricing pricing = CouponCalculator.apply(List.of(new Line(1, 99_999)), percent(10, null), 0, NOW);
        assertEquals(9_999, pricing.discountTotal());
        assertEquals(90_000, pricing.total());
    }

    @Test
    void percentage_isCappedByMaximumDiscount() {
        Pricing pricing = CouponCalculator.apply(List.of(new Line(1, 1_000_000)), percent(50, 100_000L), 0, NOW);
        assertEquals(100_000, pricing.discountTotal());
    }

    @Test
    void hundredPercent_givesZeroTotal_notNegative() {
        Pricing pricing = CouponCalculator.apply(List.of(new Line(1, 500_000)), percent(100, null), 0, NOW);
        assertEquals(0, pricing.total());
    }

    @Test
    void fixedAmount_largerThanOrder_isCappedAtEligibleSubtotal() {
        CouponRule fixed = rule(DiscountType.FIXED_AMOUNT, 1_000_000, 0, null, CouponScope.ALL_COURSES, Set.of());
        Pricing pricing = CouponCalculator.apply(List.of(new Line(1, 300_000)), fixed, 0, NOW);
        assertEquals(300_000, pricing.discountTotal());
        assertEquals(0, pricing.total());
    }

    @Test
    void specificCourses_discountOnlyEligibleItems() {
        CouponRule specific = rule(DiscountType.PERCENTAGE, 50, 0, null, CouponScope.SPECIFIC_COURSES, Set.of(2L));
        Pricing pricing = CouponCalculator.apply(List.of(new Line(1, 300_000), new Line(2, 200_000)), specific, 0,
                NOW);
        assertEquals(100_000, pricing.discountTotal());
        assertEquals(0, pricing.discountFor(1));
        assertEquals(100_000, pricing.discountFor(2));
        assertEquals(400_000, pricing.total());
    }

    @Test
    void allocation_sumsExactly_andGivesLeftoverByLargestRemainder() {
        Pricing pricing = CouponCalculator.apply(
                List.of(new Line(1, 100_000), new Line(2, 100_000), new Line(3, 100_000)),
                rule(DiscountType.FIXED_AMOUNT, 100_000, 0, null, CouponScope.ALL_COURSES, Set.of()), 0, NOW);
        assertEquals(100_000, sumOfItems(pricing));
        // 100000/3 = 33333 dư 1: đồng lẻ cho course_id nhỏ nhất khi phần dư và giá bằng nhau
        assertEquals(33_334, pricing.discountFor(1));
        assertEquals(33_333, pricing.discountFor(2));
        assertEquals(33_333, pricing.discountFor(3));
    }

    @Test
    void allocation_neverDiscountsAnItemBeyondItsPrice() {
        // Dồn đồng lẻ vào item giá cao nhất sẽ hỏng ở ca này; chia theo phần dư lớn nhất thì không
        Pricing pricing = CouponCalculator.apply(List.of(new Line(1, 1), new Line(2, 1), new Line(3, 1)),
                rule(DiscountType.FIXED_AMOUNT, 2, 0, null, CouponScope.ALL_COURSES, Set.of()), 0, NOW);
        assertEquals(2, sumOfItems(pricing));
        for (long courseId = 1; courseId <= 3; courseId++) {
            assertTrue(pricing.discountFor(courseId) <= 1);
        }
    }

    @Test
    void allocation_randomCarts_alwaysSumExactlyAndStayWithinPrice() {
        Random random = new Random(7);
        for (int run = 0; run < 2_000; run++) {
            List<Line> lines = new ArrayList<>();
            int count = 1 + random.nextInt(6);
            for (int i = 0; i < count; i++) {
                lines.add(new Line(i + 1, random.nextInt(3) == 0 ? random.nextInt(5) : random.nextInt(2_000_000)));
            }
            CouponRule coupon = random.nextBoolean() ? percent(1 + random.nextInt(100), null)
                    : rule(DiscountType.FIXED_AMOUNT, 1 + random.nextInt(3_000_000), 0, null,
                            CouponScope.ALL_COURSES, Set.of());
            Pricing pricing = CouponCalculator.apply(lines, coupon, 0, NOW);
            assertEquals(pricing.discountTotal(), sumOfItems(pricing));
            assertEquals(pricing.subtotal() - pricing.discountTotal(), pricing.total());
            assertTrue(pricing.total() >= 0);
            for (Line line : lines) {
                long discount = pricing.discountFor(line.courseId());
                assertTrue(discount >= 0 && discount <= line.price());
            }
        }
    }

    @Test
    void rejection_inactiveOrNotStarted_isInvalid() {
        CouponRule inactive = new CouponRule("X", DiscountType.PERCENTAGE, 10, 0, null, null, null, 0,
                CouponScope.ALL_COURSES, Set.of(), START, END, CouponStatus.INACTIVE);
        CouponRule notStarted = new CouponRule("X", DiscountType.PERCENTAGE, 10, 0, null, null, null, 0,
                CouponScope.ALL_COURSES, Set.of(), NOW.plusSeconds(1), END, CouponStatus.ACTIVE);
        List<Line> cart = List.of(new Line(1, 100));
        assertEquals(Optional.of(ErrorCode.COUPON_INVALID), CouponCalculator.rejectionReason(cart, inactive, 0, NOW));
        assertEquals(Optional.of(ErrorCode.COUPON_INVALID),
                CouponCalculator.rejectionReason(cart, notStarted, 0, NOW));
    }

    @Test
    void rejection_atOrAfterEnd_isExpired() {
        CouponRule ended = new CouponRule("X", DiscountType.PERCENTAGE, 10, 0, null, null, null, 0,
                CouponScope.ALL_COURSES, Set.of(), START, NOW, CouponStatus.ACTIVE);
        assertEquals(Optional.of(ErrorCode.COUPON_EXPIRED),
                CouponCalculator.rejectionReason(List.of(new Line(1, 100)), ended, 0, NOW));
    }

    @Test
    void rejection_usageLimits() {
        CouponRule total = new CouponRule("X", DiscountType.PERCENTAGE, 10, 0, null, 5, null, 5,
                CouponScope.ALL_COURSES, Set.of(), START, END, CouponStatus.ACTIVE);
        CouponRule perUser = new CouponRule("X", DiscountType.PERCENTAGE, 10, 0, null, null, 1, 0,
                CouponScope.ALL_COURSES, Set.of(), START, END, CouponStatus.ACTIVE);
        List<Line> cart = List.of(new Line(1, 100));
        assertEquals(Optional.of(ErrorCode.COUPON_USAGE_EXCEEDED),
                CouponCalculator.rejectionReason(cart, total, 0, NOW));
        assertEquals(Optional.of(ErrorCode.COUPON_USAGE_EXCEEDED),
                CouponCalculator.rejectionReason(cart, perUser, 1, NOW));
        assertEquals(Optional.empty(), CouponCalculator.rejectionReason(cart, perUser, 0, NOW));
    }

    @Test
    void rejection_minimumOrderOrNoEligibleCourse_isNotApplicable() {
        CouponRule minimum = rule(DiscountType.PERCENTAGE, 10, 1_000_000, null, CouponScope.ALL_COURSES, Set.of());
        CouponRule otherCourse = rule(DiscountType.PERCENTAGE, 10, 0, null, CouponScope.SPECIFIC_COURSES, Set.of(9L));
        List<Line> cart = List.of(new Line(1, 500_000));
        assertEquals(Optional.of(ErrorCode.COUPON_NOT_APPLICABLE),
                CouponCalculator.rejectionReason(cart, minimum, 0, NOW));
        assertEquals(Optional.of(ErrorCode.COUPON_NOT_APPLICABLE),
                CouponCalculator.rejectionReason(cart, otherCourse, 0, NOW));
    }

    @Test
    void apply_throwsBusinessExceptionWithTheRejectionCode() {
        CouponRule ended = new CouponRule("X", DiscountType.PERCENTAGE, 10, 0, null, null, null, 0,
                CouponScope.ALL_COURSES, Set.of(), START, NOW, CouponStatus.ACTIVE);
        BusinessException error = assertThrows(BusinessException.class,
                () -> CouponCalculator.apply(List.of(new Line(1, 100)), ended, 0, NOW));
        assertEquals(ErrorCode.COUPON_EXPIRED, error.code());
        assertEquals(422, error.httpStatus());
    }
}
