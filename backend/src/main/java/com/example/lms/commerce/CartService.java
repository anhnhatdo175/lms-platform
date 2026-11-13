package com.example.lms.commerce;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.commerce.CouponCalculator.Line;
import com.example.lms.commerce.CouponCalculator.Pricing;
import com.example.lms.commerce.dto.CartResponse;
import com.example.lms.commerce.dto.CartResponse.CartItemResponse;
import com.example.lms.commerce.dto.CouponPreviewResponse;
import com.example.lms.contract.course.CourseQueryService;
import com.example.lms.contract.course.CourseQueryService.CourseSnapshot;
import com.example.lms.enrollment.EnrollmentAccessService;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/** Giỏ hàng (Spec 10.1). */
@Service
public class CartService {

    private final CartRepository carts;
    private final CourseQueryService courses;
    private final EnrollmentAccessService enrollmentAccess;
    private final CouponService coupons;
    private final Clock clock;

    public CartService(CartRepository carts, CourseQueryService courses, EnrollmentAccessService enrollmentAccess,
            CouponService coupons, Clock clock) {
        this.carts = carts;
        this.courses = courses;
        this.enrollmentAccess = enrollmentAccess;
        this.coupons = coupons;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(long userId) {
        return carts.findByUserId(userId).map(this::toResponse).orElseGet(CartResponse::empty);
    }

    @Transactional
    public CartResponse addItem(long userId, long courseId) {
        CourseSnapshot course = courses.findCourse(courseId).filter(CourseSnapshot::purchasable)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_PUBLISHED,
                        "Course " + courseId + " is not for sale"));
        if (enrollmentAccess.hasActiveAccess(userId, course.id())) {
            throw new BusinessException(ErrorCode.COURSE_ALREADY_ENROLLED, "Course " + courseId + " is already owned");
        }
        Instant now = clock.instant();
        carts.insertIfAbsent(userId, now);
        // Khóa Cart để hai lần thêm song song không đụng UNIQUE(cart_id, course_id) thành lỗi 500
        Cart cart = carts.findByUserIdForUpdate(userId).orElseThrow();
        cart.addCourse(courseId, now);
        return toResponse(cart);
    }

    @Transactional
    public void removeItem(long userId, long courseId) {
        Cart cart = carts.findByUserIdForUpdate(userId).orElseThrow(() -> BusinessException.notFound("Cart item"));
        if (!cart.removeCourse(courseId, clock.instant())) {
            throw BusinessException.notFound("Cart item");
        }
    }

    /** Gợi ý giảm giá cho giỏ hiện tại; Coupon không áp được thì trả {@code valid = false} kèm lý do, không ném lỗi. */
    @Transactional(readOnly = true)
    public CouponPreviewResponse previewCoupon(long userId, String couponCode) {
        Cart cart = carts.findByUserId(userId).filter(c -> !c.isEmpty())
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_EMPTY, "Cart is empty"));
        List<Line> lines = currentLines(cart);
        String code = Coupon.normalizeCode(couponCode);
        Optional<CouponService.LoadedCoupon> loaded = coupons.load(code, userId);
        if (loaded.isEmpty()) {
            return new CouponPreviewResponse(false, code, null, null, null, ErrorCode.COUPON_INVALID);
        }
        CouponService.LoadedCoupon coupon = loaded.get();
        Instant now = clock.instant();
        Optional<ErrorCode> rejection = CouponCalculator.rejectionReason(lines, coupon.rule(),
                coupon.userRedemptions(), now);
        if (rejection.isPresent()) {
            return new CouponPreviewResponse(false, code, null, null, null, rejection.get());
        }
        Pricing pricing = CouponCalculator.apply(lines, coupon.rule(), coupon.userRedemptions(), now);
        return new CouponPreviewResponse(true, code, pricing.subtotal(), pricing.discountTotal(), pricing.total(),
                null);
    }

    private List<Line> currentLines(Cart cart) {
        Map<Long, CourseSnapshot> byId = snapshots(cart);
        return cart.getItems().stream().map(item -> byId.get(item.getCourseId())).filter(c -> c != null)
                .map(c -> new Line(c.id(), c.salePrice())).toList();
    }

    private Map<Long, CourseSnapshot> snapshots(Cart cart) {
        return courses.findCourses(cart.getItems().stream().map(CartItem::getCourseId).toList()).stream()
                .collect(Collectors.toMap(CourseSnapshot::id, Function.identity()));
    }

    private CartResponse toResponse(Cart cart) {
        Map<Long, CourseSnapshot> byId = snapshots(cart);
        List<CartItemResponse> items = cart.getItems().stream().map(item -> byId.get(item.getCourseId()))
                .filter(c -> c != null)
                .map(c -> new CartItemResponse(c.id(), c.slug(), c.title(), c.salePrice(), c.thumbnailUrl()))
                .toList();
        long subtotal = items.stream().mapToLong(CartItemResponse::price).sum();
        return new CartResponse(items, subtotal, items.size(), cart.getUpdatedAt());
    }
}
