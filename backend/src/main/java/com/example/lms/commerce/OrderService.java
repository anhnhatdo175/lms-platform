package com.example.lms.commerce;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.commerce.dto.OrderDetailResponse;
import com.example.lms.commerce.dto.OrderSummaryResponse;
import com.example.lms.commerce.dto.PaymentStatusResponse;
import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.contract.identity.UserQueryService;
import com.example.lms.payment.Payment;
import com.example.lms.payment.PaymentRepository;
import com.example.lms.payment.PaymentStatus;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/** Lịch sử, chi tiết, trạng thái thanh toán và hủy Order. */
@Service
public class OrderService {

    /** Ngày trong bộ lọc của Admin hiểu theo giờ Việt Nam. */
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final UserQueryService users;
    private final Clock clock;

    public OrderService(OrderRepository orders, PaymentRepository payments, UserQueryService users, Clock clock) {
        this.orders = orders;
        this.payments = payments;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> listMine(long userId, Pageable pageable) {
        return orders.findByUserId(userId, pageable).map(OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse get(CurrentUser user, long orderId) {
        Order order = readable(user, orderId);
        return OrderDetailResponse.from(order, payments.findFirstByOrderIdOrderByIdDesc(orderId).orElse(null));
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse paymentStatus(CurrentUser user, long orderId) {
        Order order = readable(user, orderId);
        Payment payment = payments.findFirstByOrderIdOrderByIdDesc(orderId)
                .orElseThrow(() -> BusinessException.notFound("Payment"));
        return PaymentStatusResponse.from(order, payment);
    }

    /** Spec 10.6: Order CANCELLED và Payment CANCELLED ngay; không cho hủy khi Payment đang REVIEW_REQUIRED. */
    @Transactional
    public OrderDetailResponse cancel(long userId, long orderId) {
        Order order = orders.findByIdForUpdate(orderId).filter(o -> o.getUserId() == userId)
                .orElseThrow(() -> BusinessException.notFound("Order"));
        Payment payment = payments.findLatestIdByOrderId(orderId)
                .flatMap(payments::findByIdForUpdate).orElse(null);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                || (payment != null && payment.getStatus() == PaymentStatus.REVIEW_REQUIRED)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE,
                    "Order " + order.getCode() + " cannot be cancelled");
        }
        Instant now = clock.instant();
        order.cancel(now);
        if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
            payment.cancel(now);
        }
        return OrderDetailResponse.from(order, payment);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> searchForAdmin(CurrentUser admin, OrderStatus status, String q,
            LocalDate from, LocalDate to, Pageable pageable) {
        requireAdmin(admin);
        String code = q == null || q.isBlank() ? null : q.trim();
        Long userId = code == null ? null : users.findIdByEmail(code).orElse(null);
        Instant fromInstant = from == null ? null : from.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        return orders.searchForAdmin(status, code, userId, fromInstant, toInstant, pageable)
                .map(OrderSummaryResponse::from);
    }

    /** Chủ Order hoặc Admin; Order của người khác trả 404 để không lộ sự tồn tại (Spec 7.4). */
    private Order readable(CurrentUser user, long orderId) {
        Order order = orders.findById(orderId).orElseThrow(() -> BusinessException.notFound("Order"));
        if (order.getUserId() != user.id() && !user.isAdmin()) {
            throw BusinessException.notFound("Order");
        }
        order.getItems().size();
        return order;
    }

    static void requireAdmin(CurrentUser user) {
        if (!user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Administrator role required");
        }
    }
}
