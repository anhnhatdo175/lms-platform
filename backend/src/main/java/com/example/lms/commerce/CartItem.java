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

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false, updatable = false)
    private Cart cart;

    @Column(name = "course_id", nullable = false, updatable = false)
    private Long courseId;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    protected CartItem() {
    }

    CartItem(Cart cart, long courseId, Instant addedAt) {
        this.cart = cart;
        this.courseId = courseId;
        this.addedAt = addedAt;
    }

    public Long getId() {
        return id;
    }

    public long getCourseId() {
        return courseId;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
