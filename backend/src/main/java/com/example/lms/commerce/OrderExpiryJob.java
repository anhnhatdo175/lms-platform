package com.example.lms.commerce;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.lms.payment.PaymentProperties;
import com.example.lms.payment.PaymentRepository;
import com.example.lms.payment.PaymentStatus;

/**
 * Đóng Order chờ thanh toán đã quá {@code expires_at + grace} (Spec 10.6). Mỗi Order một transaction riêng,
 * khóa Order rồi Payment như mọi luồng tiền khác, và kiểm tra lại điều kiện sau khi đã có khóa.
 */
@Component
public class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);
    private static final int BATCH = 100;

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final PaymentProperties properties;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public OrderExpiryJob(OrderRepository orders, PaymentRepository payments, PaymentProperties properties,
            TransactionTemplate transactions, Clock clock) {
        this.orders = orders;
        this.payments = payments;
        this.properties = properties;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${payment.order-expiry-job-delay:PT1M}")
    public void expireOverdueOrders() {
        Instant now = clock.instant();
        List<Long> ids = orders.findExpirableIds(OrderStatus.PENDING_PAYMENT, PaymentStatus.REVIEW_REQUIRED,
                now.minus(properties.lateGrace()), PageRequest.of(0, BATCH));
        for (Long id : ids) {
            try {
                transactions.executeWithoutResult(status -> expireOne(id, now));
            } catch (RuntimeException e) {
                log.warn("Could not expire order {}", id, e);
            }
        }
    }

    private void expireOne(long orderId, Instant now) {
        Order order = orders.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.acceptsAutomaticPaymentAt(now, properties.lateGrace())
                || order.getStatus() != OrderStatus.PENDING_PAYMENT
                || payments.existsByOrderIdAndStatus(orderId, PaymentStatus.REVIEW_REQUIRED)) {
            return;
        }
        order.expire(now);
        payments.findLatestIdByOrderId(orderId).flatMap(payments::findByIdForUpdate)
                .filter(p -> p.getStatus() == PaymentStatus.PENDING).ifPresent(p -> p.cancel(now));
    }
}
