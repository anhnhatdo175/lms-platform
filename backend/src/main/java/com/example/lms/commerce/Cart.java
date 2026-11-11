package com.example.lms.commerce;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Mỗi User một Cart (Spec 10.1). Cart không lưu giá: giá luôn tính lại lúc checkout. */
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("addedAt ASC")
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Cart() {
    }

    public Cart(long userId, Instant now) {
        this.userId = userId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean contains(long courseId) {
        return items.stream().anyMatch(item -> item.getCourseId() == courseId);
    }

    public CartItem addCourse(long courseId, Instant now) {
        if (contains(courseId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Course " + courseId + " is already in the cart");
        }
        CartItem item = new CartItem(this, courseId, now);
        items.add(item);
        updatedAt = now;
        return item;
    }

    public boolean removeCourse(long courseId, Instant now) {
        boolean removed = items.removeIf(item -> item.getCourseId() == courseId);
        if (removed) {
            updatedAt = now;
        }
        return removed;
    }

    public void removeCourses(Collection<Long> courseIds, Instant now) {
        if (items.removeIf(item -> courseIds.contains(item.getCourseId()))) {
            updatedAt = now;
        }
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
