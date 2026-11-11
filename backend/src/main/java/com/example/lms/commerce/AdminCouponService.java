package com.example.lms.commerce;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.commerce.dto.CouponResponse;
import com.example.lms.commerce.dto.CreateCouponRequest;
import com.example.lms.commerce.dto.UpdateCouponRequest;
import com.example.lms.contract.course.CourseQueryService;
import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/** Quản lý Coupon của Admin (SHOULD, Spec 5.2). */
@Service
public class AdminCouponService {

    private final CouponRepository coupons;
    private final CouponCourseRepository couponCourses;
    private final CourseQueryService courses;
    private final Clock clock;

    public AdminCouponService(CouponRepository coupons, CouponCourseRepository couponCourses,
            CourseQueryService courses, Clock clock) {
        this.coupons = coupons;
        this.couponCourses = couponCourses;
        this.courses = courses;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<CouponResponse> list(CurrentUser admin, CouponStatus status, String q, Pageable pageable) {
        OrderService.requireAdmin(admin);
        String query = q == null || q.isBlank() ? null : q.trim();
        return coupons.search(status, query, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CouponResponse get(CurrentUser admin, long couponId) {
        OrderService.requireAdmin(admin);
        return toResponse(find(couponId));
    }

    @Transactional
    public CouponResponse create(CurrentUser admin, CreateCouponRequest request) {
        OrderService.requireAdmin(admin);
        if (request.discountType() == DiscountType.PERCENTAGE && request.discountValue() > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Percentage discount must be 1-100");
        }
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "endsAt must be after startsAt");
        }
        Set<Long> courseIds = request.courseIds() == null ? Set.of() : new HashSet<>(request.courseIds());
        if (request.scope() == CouponScope.SPECIFIC_COURSES) {
            if (courseIds.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "courseIds is required for SPECIFIC_COURSES");
            }
            if (courses.findCourses(courseIds).size() != courseIds.size()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Some courseIds do not exist");
            }
        }
        String code = Coupon.normalizeCode(request.code());
        if (coupons.findByCode(code).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Coupon code " + code + " already exists");
        }
        Instant now = clock.instant();
        Coupon coupon = coupons.save(new Coupon(code, request.name(), request.discountType(), request.discountValue(),
                request.minimumOrderAmount() == null ? 0 : request.minimumOrderAmount(),
                request.maximumDiscountAmount(), request.totalUsageLimit(), request.perUserLimit(), request.scope(),
                request.startsAt(), request.endsAt(), now));
        if (request.scope() == CouponScope.SPECIFIC_COURSES) {
            courseIds.forEach(courseId -> couponCourses.save(new CouponCourse(coupon.getId(), courseId, now)));
        }
        return CouponResponse.from(coupon, List.copyOf(courseIds));
    }

    @Transactional
    public CouponResponse update(CurrentUser admin, long couponId, UpdateCouponRequest request) {
        OrderService.requireAdmin(admin);
        Coupon coupon = find(couponId);
        coupon.update(request.name(), request.status(), request.endsAt(), request.totalUsageLimit(),
                request.perUserLimit(), clock.instant());
        return toResponse(coupon);
    }

    private Coupon find(long couponId) {
        return coupons.findById(couponId).orElseThrow(() -> BusinessException.notFound("Coupon"));
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.from(coupon,
                couponCourses.findByCouponId(coupon.getId()).stream().map(CouponCourse::getCourseId).toList());
    }
}
