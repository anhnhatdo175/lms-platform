package com.example.lms.commerce;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.lms.commerce.CouponCalculator.Line;
import com.example.lms.commerce.CouponCalculator.Pricing;
import com.example.lms.commerce.dto.CheckoutRequest;
import com.example.lms.contract.course.CourseQueryService;
import com.example.lms.contract.course.CourseQueryService.CourseSnapshot;
import com.example.lms.enrollment.EnrollmentAccessService;
import com.example.lms.payment.Payment;
import com.example.lms.payment.PaymentMethod;
import com.example.lms.payment.PaymentProperties;
import com.example.lms.payment.PaymentProviderType;
import com.example.lms.payment.PaymentReferenceGenerator;
import com.example.lms.payment.PaymentRepository;
import com.example.lms.payment.QrCodeUrlBuilder;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Checkout (Spec 10.7, ERD Mục 14): một transaction tạo Order, OrderItem snapshot và Payment PENDING.
 * Không tăng {@code used_count} của Coupon ở đây; chỉ tăng khi thanh toán thành công.
 */
@Service
public class CheckoutService {

    private final CartRepository carts;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final PaymentRepository payments;
    private final CouponService coupons;
    private final CourseQueryService courses;
    private final EnrollmentAccessService enrollmentAccess;
    private final PaymentReferenceGenerator references;
    private final OrderCodeGenerator orderCodes;
    private final PaymentProperties properties;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public CheckoutService(CartRepository carts, OrderRepository orders, OrderItemRepository orderItems,
            PaymentRepository payments, CouponService coupons, CourseQueryService courses,
            EnrollmentAccessService enrollmentAccess, PaymentReferenceGenerator references,
            OrderCodeGenerator orderCodes, PaymentProperties properties, TransactionTemplate transactions,
            Clock clock) {
        this.carts = carts;
        this.orders = orders;
        this.orderItems = orderItems;
        this.payments = payments;
        this.coupons = coupons;
        this.courses = courses;
        this.enrollmentAccess = enrollmentAccess;
        this.references = references;
        this.orderCodes = orderCodes;
        this.properties = properties;
        this.transactions = transactions;
        this.clock = clock;
    }

    /** @param created {@code false} khi trả lại Order cũ của cùng Idempotency-Key (HTTP 200 thay vì 201) */
    public record CheckoutOutcome(Order order, Payment payment, boolean created) {
    }

    public CheckoutOutcome checkout(long userId, String idempotencyKey, CheckoutRequest request) {
        String key = requireUuid(idempotencyKey);
        String couponCode = request == null ? null : request.couponCode();
        String hash = CheckoutRequestHasher.hash(couponCode, request == null ? null : request.paymentMethod());

        Optional<CheckoutOutcome> replay = findReplay(userId, key, hash);
        if (replay.isPresent()) {
            return replay.get();
        }
        try {
            return transactions.execute(status -> createOrder(userId, key, hash, couponCode));
        } catch (DataIntegrityViolationException raced) {
            // Hai request cùng key chạy song song: UNIQUE(user_id, checkout_idempotency_key) đã chặn bên sau,
            // transaction của bên sau đã rollback nên đọc lại trong transaction mới.
            return findReplay(userId, key, hash).orElseThrow(() -> raced);
        }
    }

    private Optional<CheckoutOutcome> findReplay(long userId, String key, String hash) {
        return transactions.execute(status -> orders.findByUserIdAndCheckoutIdempotencyKey(userId, key)
                .map(existing -> replayOf(existing, hash)));
    }

    private CheckoutOutcome replayOf(Order existing, String hash) {
        if (!existing.getCheckoutRequestHash().equals(hash)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                    "Idempotency-Key was already used with a different request");
        }
        existing.getItems().size();
        Payment payment = payments.findFirstByOrderIdOrderByIdDesc(existing.getId()).orElseThrow();
        return new CheckoutOutcome(existing, payment, false);
    }

    private CheckoutOutcome createOrder(long userId, String key, String hash, String couponCode) {
        Instant now = clock.instant();
        Duration grace = properties.lateGrace();

        // Khóa Cart: hai lần checkout của cùng User (kể cả khác key, ví dụ bấm đúp) chạy tuần tự
        Cart cart = carts.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_EMPTY, "Cart is empty"));
        // Kiểm tra lại key sau khi có khóa: request cùng key chạy trước có thể vừa tạo Order và xóa giỏ
        Optional<Order> sameKey = orders.findByUserIdAndCheckoutIdempotencyKey(userId, key);
        if (sameKey.isPresent()) {
            return replayOf(sameKey.get(), hash);
        }
        if (cart.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY, "Cart is empty");
        }
        List<Long> courseIds = cart.getItems().stream().map(CartItem::getCourseId).toList();
        Map<Long, CourseSnapshot> snapshots = courses.findCourses(courseIds).stream()
                .collect(Collectors.toMap(CourseSnapshot::id, Function.identity()));
        for (long courseId : courseIds) {
            CourseSnapshot course = snapshots.get(courseId);
            if (course == null || !course.purchasable()) {
                throw new BusinessException(ErrorCode.COURSE_NOT_PUBLISHED, "Course " + courseId + " is not for sale");
            }
            if (enrollmentAccess.hasActiveAccess(userId, courseId)) {
                throw new BusinessException(ErrorCode.COURSE_ALREADY_ENROLLED,
                        "Course " + courseId + " is already owned");
            }
        }
        if (orderItems.existsInOpenOrder(userId, OrderStatus.PENDING_PAYMENT, now.minus(grace), courseIds)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "A course in the cart is already in another order awaiting payment");
        }

        List<Line> lines = courseIds.stream().map(id -> new Line(id, snapshots.get(id).salePrice())).toList();
        CouponService.LoadedCoupon coupon = null;
        Pricing pricing;
        if (couponCode == null || couponCode.isBlank()) {
            pricing = CouponCalculator.withoutCoupon(lines);
        } else {
            coupon = coupons.load(couponCode, userId).orElseThrow(
                    () -> new BusinessException(ErrorCode.COUPON_INVALID, "Coupon not found"));
            pricing = CouponCalculator.apply(lines, coupon.rule(), coupon.userRedemptions(), now);
        }
        // Order 0 đ không thể trả bằng chuyển khoản; Course miễn phí do Admin cấp quyền (Doc phần B, Mục 13 ý 4)
        if (pricing.subtotal() == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Free courses are granted by an administrator");
        }
        if (pricing.total() == 0) {
            throw new BusinessException(ErrorCode.COUPON_NOT_APPLICABLE, "Coupon would make the order free");
        }

        Order order = new Order(orderCodes.next(now), userId, coupon == null ? null : coupon.coupon().getId(),
                coupon == null ? null : coupon.coupon().getCode(), pricing.subtotal(), pricing.discountTotal(), key,
                hash, now.plus(properties.orderPaymentWindow()), now);
        for (long courseId : courseIds) {
            CourseSnapshot course = snapshots.get(courseId);
            order.addItem(courseId, course.title(), course.salePrice(), pricing.discountFor(courseId),
                    course.accessDurationDays(), now);
        }
        orders.saveAndFlush(order);

        String reference = references.next();
        Payment payment = Payment.pending(order.getId(), PaymentProviderType.SEPAY, PaymentMethod.BANK_QR,
                order.getTotalAmount(), reference, QrCodeUrlBuilder.build(properties.sepay(),
                        order.getTotalAmount(), reference), now);
        payments.saveAndFlush(payment);

        // Xóa ngay các Course đã đặt khỏi Cart (Doc phần B, Mục 13 ý 5)
        cart.removeCourses(courseIds, now);
        return new CheckoutOutcome(order, payment, true);
    }

    private static String requireUuid(String key) {
        try {
            return UUID.fromString(key == null ? "" : key.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key header must be a UUID");
        }
    }
}
