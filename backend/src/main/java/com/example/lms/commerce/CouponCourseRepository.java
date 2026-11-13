package com.example.lms.commerce;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponCourseRepository extends JpaRepository<CouponCourse, CouponCourse.Key> {

    List<CouponCourse> findByCouponId(long couponId);
}
