package com.example.lms.commerce;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.lms.shared.exception.BusinessException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Đơn hàng. Tên entity là {@code PurchaseOrder} vì {@code ORDER} là từ khóa của JPQL;
 * bảng vẫn là {@code orders} theo ERD.
 */
@Entity(name = "PurchaseOrder")
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = 50)
    private String code;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "coupon_id", updatable = false)
    private Long couponId;

    @Column(name = "coupon_code", updatable = false, length = 50)
    private String couponCode;

    @Column(nullable = false, updatable = false)
    private long subtotal;

    @Column(name = "discount_amount", nullable = false, updatable = false)
    private long discountAmount;

    @Column(name = "total_amount", nullable = false, updatable = false)
    private long totalAmount;

    @Column(name = "checkout_idempotency_key", nullable = false, updatable = false, length = 64)
    private String checkoutIdempotencyKey;

    @Column(name = "checkout_request_hash", nullable = false, updatable = false, length = 64)
    private String checkoutRequestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(String code, long userId, Long couponId, String couponCode, long subtotal, long discountAmount,
            String checkoutIdempotencyKey, String checkoutRequestHash, Instant expiresAt, Instant now) {
        if (discountAmount > subtotal) {
            throw new IllegalArgumentException("discount must not exceed subtotal");
        }
        this.code = code;
        this.userId = userId;
        this.couponId = couponId;
        this.couponCode = couponCode;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.totalAmount = subtotal - discountAmount;
        this.checkoutIdempotencyKey = checkoutIdempotencyKey;
        this.checkoutRequestHash = checkoutRequestHash;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.expiresAt = expiresAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Snapshot giá và tên Course lúc mua (Spec 10.3); đổi giá Course sau đó không ảnh hưởng Order. */
    public OrderItem addItem(long courseId, String courseTitle, long unitPrice, long itemDiscount,
            Integer accessDurationDays, Instant now) {
        OrderItem item = new OrderItem(this, courseId, courseTitle, unitPrice, itemDiscount, accessDurationDays, now);
        items.add(item);
        return item;
    }

    /**
     * Còn nhận thanh toán tự động tại thời điểm {@code at}: đang chờ và chưa quá {@code expiresAt + grace}
     * (Spec 10.6, DEC-005).
     */
    public boolean acceptsAutomaticPaymentAt(Instant at, Duration grace) {
        return status == OrderStatus.PENDING_PAYMENT && !at.isAfter(expiresAt.plus(grace));
    }

    public void markPaid(Instant now) {
        transitionTo(OrderStatus.PAID, false);
        paidAt = now;
        updatedAt = now;
    }

    /** Chỉ dùng khi Admin approve đối soát: cho phép thêm EXPIRED/CANCELLED → PAID (Spec 10.5). */
    public void markPaidByReconciliation(Instant now) {
        transitionTo(OrderStatus.PAID, true);
        paidAt = now;
        updatedAt = now;
    }

    public void cancel(Instant now) {
        transitionTo(OrderStatus.CANCELLED, false);
        cancelledAt = now;
        updatedAt = now;
    }

    public void expire(Instant now) {
        transitionTo(OrderStatus.EXPIRED, false);
        updatedAt = now;
    }

    private void transitionTo(OrderStatus target, boolean viaReconciliation) {
        boolean allowed = viaReconciliation ? status.canTransitionByReconciliation(target)
                : status.canTransitionTo(target);
        if (!allowed) {
            throw BusinessException.stateConflict("Order " + id + " cannot move from " + status + " to " + target);
        }
        status = target;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public long getSubtotal() {
        return subtotal;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public String getCheckoutIdempotencyKey() {
        return checkoutIdempotencyKey;
    }

    public String getCheckoutRequestHash() {
        return checkoutRequestHash;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
