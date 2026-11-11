package com.example.lms.commerce;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Ghi khi Payment thành công, không ghi lúc checkout (Spec 10.2). {@code UNIQUE(order_id)} chặn ghi hai lần. */
@Entity
@Table(name = "coupon_redemptions")
public class CouponRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false, updatable = false)
    private Long couponId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(name = "discount_amount", nullable = false, updatable = false)
    private long discountAmount;

    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private Instant redeemedAt;

    protected CouponRedemption() {
    }

    public CouponRedemption(long couponId, long userId, long orderId, long discountAmount, Instant redeemedAt) {
        this.couponId = couponId;
        this.userId = userId;
        this.orderId = orderId;
        this.discountAmount = discountAmount;
        this.redeemedAt = redeemedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getCouponId() {
        return couponId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }
}
