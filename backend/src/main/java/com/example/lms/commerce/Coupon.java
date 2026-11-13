package com.example.lms.commerce;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Coupon (Spec 10.2). {@code used_count} chỉ tăng khi Payment thành công và tăng bằng một câu UPDATE nguyên tử
 * ({@code CouponRepository.incrementUsedCount}), không đọc-sửa-ghi trên entity.
 */
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, updatable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, updatable = false)
    private long discountValue;

    @Column(name = "minimum_order_amount", nullable = false, updatable = false)
    private long minimumOrderAmount;

    @Column(name = "maximum_discount_amount", updatable = false)
    private Long maximumDiscountAmount;

    @Column(name = "total_usage_limit")
    private Integer totalUsageLimit;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "used_count", nullable = false, insertable = false, updatable = false)
    private int usedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CouponScope scope;

    @Column(name = "starts_at", nullable = false, updatable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Coupon() {
    }

    public Coupon(String code, String name, DiscountType discountType, long discountValue, long minimumOrderAmount,
            Long maximumDiscountAmount, Integer totalUsageLimit, Integer perUserLimit, CouponScope scope,
            Instant startsAt, Instant endsAt, Instant now) {
        this.code = normalizeCode(code);
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderAmount = minimumOrderAmount;
        this.maximumDiscountAmount = maximumDiscountAmount;
        this.totalUsageLimit = totalUsageLimit;
        this.perUserLimit = perUserLimit;
        this.scope = scope;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = CouponStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Mã so khớp không phân biệt hoa thường; lưu và tra cứu đều ở dạng viết hoa, bỏ khoảng trắng hai đầu. */
    public static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    /** OpenAPI UpdateCouponRequest: không đổi mã, loại, giá trị giảm sau khi tạo; trường {@code null} giữ nguyên. */
    public void update(String name, CouponStatus status, Instant endsAt, Integer totalUsageLimit,
            Integer perUserLimit, Instant now) {
        if (name != null) {
            this.name = name;
        }
        if (status != null) {
            this.status = status;
        }
        if (endsAt != null) {
            this.endsAt = endsAt;
        }
        if (totalUsageLimit != null) {
            this.totalUsageLimit = totalUsageLimit;
        }
        if (perUserLimit != null) {
            this.perUserLimit = perUserLimit;
        }
        if (!this.endsAt.isAfter(this.startsAt)) {
            throw new com.example.lms.shared.exception.BusinessException(
                    com.example.lms.shared.exception.ErrorCode.VALIDATION_ERROR, "endsAt must be after startsAt");
        }
        this.updatedAt = now;
    }

    /** Ảnh chụp bất biến để {@link CouponCalculator} tính mà không chạm database. */
    public CouponRule toRule(Set<Long> eligibleCourseIds) {
        return new CouponRule(code, discountType, discountValue, minimumOrderAmount, maximumDiscountAmount,
                totalUsageLimit, perUserLimit, usedCount, scope, eligibleCourseIds, startsAt, endsAt, status);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public long getDiscountValue() {
        return discountValue;
    }

    public long getMinimumOrderAmount() {
        return minimumOrderAmount;
    }

    public Long getMaximumDiscountAmount() {
        return maximumDiscountAmount;
    }

    public Integer getTotalUsageLimit() {
        return totalUsageLimit;
    }

    public Integer getPerUserLimit() {
        return perUserLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public CouponScope getScope() {
        return scope;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public CouponStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
