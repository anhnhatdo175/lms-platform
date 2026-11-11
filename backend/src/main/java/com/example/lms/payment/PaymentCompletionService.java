package com.example.lms.payment;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.commerce.CouponRedemption;
import com.example.lms.commerce.CouponRedemptionRepository;
import com.example.lms.commerce.CouponRepository;
import com.example.lms.commerce.Order;
import com.example.lms.commerce.OrderItem;
import com.example.lms.enrollment.EnrollmentGrantService;

/**
 * Đường code duy nhất ghi nhận đã thu tiền (Spec 11.9): dòng 8 của webhook và Admin approve đều gọi hàm này,
 * nên hai đường cấp quyền học không thể lệch nhau. Chạy trong transaction của bên gọi, sau khi bên gọi đã khóa
 * Order rồi Payment.
 */
@Service
public class PaymentCompletionService {

    private final EnrollmentGrantService grants;
    private final CouponRedemptionRepository redemptions;
    private final CouponRepository coupons;

    public PaymentCompletionService(EnrollmentGrantService grants, CouponRedemptionRepository redemptions,
            CouponRepository coupons) {
        this.grants = grants;
        this.redemptions = redemptions;
        this.coupons = coupons;
    }

    /**
     * @param viaReconciliation Admin approve: cho phép Order EXPIRED/CANCELLED → PAID (Spec 10.5)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(Order order, Payment payment, long receivedAmount, long webhookLogId,
            String providerTransactionId, boolean viaReconciliation, Instant now) {
        payment.markPaid(receivedAmount, webhookLogId, providerTransactionId, now);
        if (viaReconciliation) {
            order.markPaidByReconciliation(now);
        } else {
            order.markPaid(now);
        }
        for (OrderItem item : order.getItems()) {
            grants.grantForOrderItem(item, order.getUserId(), now);
        }
        if (order.getCouponId() != null && !redemptions.existsByOrderId(order.getId())) {
            redemptions.save(new CouponRedemption(order.getCouponId(), order.getUserId(), order.getId(),
                    order.getDiscountAmount(), now));
            coupons.incrementUsedCount(order.getCouponId());
        }
    }
}
