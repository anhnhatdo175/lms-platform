package com.example.lms.commerce;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Course được áp Coupon khi {@code scope = SPECIFIC_COURSES}. */
@Entity
@Table(name = "coupon_courses")
@IdClass(CouponCourse.Key.class)
public class CouponCourse {

    @Id
    @Column(name = "coupon_id")
    private Long couponId;

    @Id
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CouponCourse() {
    }

    public CouponCourse(long couponId, long courseId, Instant now) {
        this.couponId = couponId;
        this.courseId = courseId;
        this.createdAt = now;
    }

    public Long getCouponId() {
        return couponId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public static class Key implements Serializable {
        private Long couponId;
        private Long courseId;

        public Key() {
        }

        public Key(Long couponId, Long courseId) {
            this.couponId = couponId;
            this.courseId = courseId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && Objects.equals(couponId, key.couponId)
                    && Objects.equals(courseId, key.courseId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(couponId, courseId);
        }
    }
}
