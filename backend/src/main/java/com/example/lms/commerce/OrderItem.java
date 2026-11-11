package com.example.lms.commerce;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Snapshot một Course trong Order. {@code original_price} lưu giá bán lúc mua ({@code courses.sale_price}),
 * tương ứng {@code unitPrice} của API (Doc thiết kế phần B, Mục 13 ý 10).
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(name = "course_id", nullable = false, updatable = false)
    private Long courseId;

    @Column(name = "course_title", nullable = false, updatable = false)
    private String courseTitle;

    @Column(name = "original_price", nullable = false, updatable = false)
    private long originalPrice;

    @Column(name = "discount_amount", nullable = false, updatable = false)
    private long discountAmount;

    @Column(name = "final_price", nullable = false, updatable = false)
    private long finalPrice;

    @Column(name = "access_duration_days", updatable = false)
    private Integer accessDurationDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderItem() {
    }

    OrderItem(Order order, long courseId, String courseTitle, long originalPrice, long discountAmount,
            Integer accessDurationDays, Instant now) {
        if (discountAmount > originalPrice) {
            throw new IllegalArgumentException("item discount must not exceed item price");
        }
        this.order = order;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.originalPrice = originalPrice;
        this.discountAmount = discountAmount;
        this.finalPrice = originalPrice - discountAmount;
        this.accessDurationDays = accessDurationDays;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public long getCourseId() {
        return courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public long getOriginalPrice() {
        return originalPrice;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public long getFinalPrice() {
        return finalPrice;
    }

    public Integer getAccessDurationDays() {
        return accessDurationDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
