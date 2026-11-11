package com.example.lms.commerce;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Tính tiền một giỏ hàng có hoặc không có Coupon (Spec 10.2). Hàm thuần, dùng chung cho
 * {@code coupon-preview} và checkout, nên hai nơi luôn cho cùng con số.
 */
public final class CouponCalculator {

    private CouponCalculator() {
    }

    /** Một Course trong giỏ với giá bán hiện tại. */
    public record Line(long courseId, long price) {
        public Line {
            if (price < 0) {
                throw new IllegalArgumentException("price must not be negative");
            }
        }
    }

    /** {@code discountByCourse} có đủ mọi Course trong giỏ (0 nếu không được giảm) và cộng lại đúng bằng {@code discountTotal}. */
    public record Pricing(long subtotal, long discountTotal, long total, Map<Long, Long> discountByCourse) {

        public long discountFor(long courseId) {
            return discountByCourse.getOrDefault(courseId, 0L);
        }
    }

    public static Pricing withoutCoupon(List<Line> lines) {
        Map<Long, Long> none = new LinkedHashMap<>();
        lines.forEach(line -> none.put(line.courseId(), 0L));
        long subtotal = subtotal(lines);
        return new Pricing(subtotal, 0, subtotal, none);
    }

    /**
     * @param userRedemptions số lần User này đã dùng Coupon trong các Order đã thanh toán
     * @throws BusinessException với một trong các mã {@code COUPON_*}
     */
    public static Pricing apply(List<Line> lines, CouponRule rule, long userRedemptions, Instant now) {
        Optional<ErrorCode> rejection = rejectionReason(lines, rule, userRedemptions, now);
        if (rejection.isPresent()) {
            throw new BusinessException(rejection.get(), "Coupon " + rule.code() + " cannot be applied");
        }
        List<Line> eligible = lines.stream().filter(line -> rule.appliesTo(line.courseId())).toList();
        long discount = discountAmount(rule, subtotal(eligible));
        Map<Long, Long> byCourse = new LinkedHashMap<>();
        lines.forEach(line -> byCourse.put(line.courseId(), 0L));
        byCourse.putAll(allocate(discount, eligible));
        long subtotal = subtotal(lines);
        return new Pricing(subtotal, discount, subtotal - discount, byCourse);
    }

    /** Lý do không áp được Coupon, theo thứ tự kiểm tra cố định; rỗng nếu áp được. */
    public static Optional<ErrorCode> rejectionReason(List<Line> lines, CouponRule rule, long userRedemptions,
            Instant now) {
        if (rule.status() != CouponStatus.ACTIVE || now.isBefore(rule.startsAt())) {
            return Optional.of(ErrorCode.COUPON_INVALID);
        }
        if (!now.isBefore(rule.endsAt())) {
            return Optional.of(ErrorCode.COUPON_EXPIRED);
        }
        if (rule.totalUsageLimit() != null && rule.usedCount() >= rule.totalUsageLimit()) {
            return Optional.of(ErrorCode.COUPON_USAGE_EXCEEDED);
        }
        if (rule.perUserLimit() != null && userRedemptions >= rule.perUserLimit()) {
            return Optional.of(ErrorCode.COUPON_USAGE_EXCEEDED);
        }
        if (subtotal(lines) < rule.minimumOrderAmount()
                || lines.stream().noneMatch(line -> rule.appliesTo(line.courseId()))) {
            return Optional.of(ErrorCode.COUPON_NOT_APPLICABLE);
        }
        return Optional.empty();
    }

    /**
     * Làm tròn xuống để không bao giờ giảm quá mức đã hứa; luôn bị chặn bởi trần giảm và tổng tiền
     * các Course được áp, nên tổng đơn không thể âm.
     */
    static long discountAmount(CouponRule rule, long eligibleSubtotal) {
        long raw = switch (rule.discountType()) {
            case PERCENTAGE -> BigInteger.valueOf(eligibleSubtotal)
                    .multiply(BigInteger.valueOf(rule.discountValue()))
                    .divide(BigInteger.valueOf(100))
                    .longValueExact();
            case FIXED_AMOUNT -> rule.discountValue();
        };
        long capped = rule.maximumDiscountAmount() == null ? raw : Math.min(raw, rule.maximumDiscountAmount());
        return Math.min(capped, eligibleSubtotal);
    }

    /**
     * Chia tiền giảm cho từng Course theo tỷ lệ giá, dùng phương pháp phần dư lớn nhất: mỗi item nhận phần
     * nguyên, các đồng lẻ còn lại lần lượt cho item có phần dư lớn nhất (hòa thì giá cao hơn, rồi course_id nhỏ hơn).
     * Cách này luôn cộng đúng bằng {@code discount} và không item nào bị giảm quá giá của nó.
     */
    static Map<Long, Long> allocate(long discount, List<Line> eligible) {
        Map<Long, Long> result = new LinkedHashMap<>();
        long eligibleTotal = subtotal(eligible);
        if (discount == 0 || eligibleTotal == 0) {
            eligible.forEach(line -> result.put(line.courseId(), 0L));
            return result;
        }
        record Share(Line line, long base, BigInteger remainder) {
        }
        BigInteger total = BigInteger.valueOf(eligibleTotal);
        List<Share> shares = new ArrayList<>();
        long allocated = 0;
        for (Line line : eligible) {
            BigInteger[] quotientAndRemainder = BigInteger.valueOf(discount)
                    .multiply(BigInteger.valueOf(line.price()))
                    .divideAndRemainder(total);
            long base = quotientAndRemainder[0].longValueExact();
            shares.add(new Share(line, base, quotientAndRemainder[1]));
            allocated += base;
        }
        long leftover = discount - allocated;
        shares.sort(Comparator.comparing(Share::remainder).reversed()
                .thenComparing(share -> share.line().price(), Comparator.reverseOrder())
                .thenComparing(share -> share.line().courseId()));
        for (int i = 0; i < shares.size(); i++) {
            Share share = shares.get(i);
            result.put(share.line().courseId(), share.base() + (i < leftover ? 1 : 0));
        }
        return result;
    }

    private static long subtotal(List<Line> lines) {
        return lines.stream().mapToLong(Line::price).sum();
    }
}
