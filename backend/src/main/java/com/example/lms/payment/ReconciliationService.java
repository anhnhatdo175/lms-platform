package com.example.lms.payment;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.commerce.Order;
import com.example.lms.commerce.OrderRepository;
import com.example.lms.contract.audit.AuditLogService;
import com.example.lms.contract.audit.AuditLogService.AuditEntry;
import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.payment.PaymentDecision.Outcome;
import com.example.lms.payment.dto.ReconciliationItemResponse;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Hàng đợi đối soát và thao tác của Admin (Spec 11.5, 11.6). Mọi thao tác: chỉ Admin, bắt buộc lý do, khóa
 * Order → Payment → webhook log, ghi {@code payment_reconciliation_actions} và AuditLog trong cùng transaction.
 */
@Service
public class ReconciliationService {

    private final PaymentWebhookLogRepository logs;
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final PaymentReconciliationActionRepository actions;
    private final PaymentCompletionService completion;
    private final AuditLogService audit;
    private final PaymentProperties properties;
    private final Clock clock;

    public ReconciliationService(PaymentWebhookLogRepository logs, PaymentRepository payments,
            OrderRepository orders, PaymentReconciliationActionRepository actions,
            PaymentCompletionService completion, AuditLogService audit, PaymentProperties properties, Clock clock) {
        this.logs = logs;
        this.payments = payments;
        this.orders = orders;
        this.actions = actions;
        this.completion = completion;
        this.audit = audit;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationItemResponse> queue(CurrentUser admin, Pageable pageable) {
        requireAdmin(admin);
        return withDetails(logs.findReconciliationQueue(WebhookProcessingStatus.UNMATCHED,
                ReconciliationActionType.REJECT, PaymentStatus.REVIEW_REQUIRED, pageable));
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationItemResponse> webhooks(CurrentUser admin, Pageable pageable) {
        requireAdmin(admin);
        return withDetails(logs.findAllBy(pageable));
    }

    /** Payment REVIEW_REQUIRED → PAID với số tiền thực nhận; Order PAID kể cả từ EXPIRED/CANCELLED (AC-PAY-05). */
    @Transactional
    public ReconciliationItemResponse approve(CurrentUser admin, long webhookId, String reason) {
        requireAdmin(admin);
        Locked locked = lockReview(webhookId);
        Instant now = clock.instant();
        PaymentStatus before = locked.payment().getStatus();
        long received = locked.payment().getReceivedAmount() == null ? 0 : locked.payment().getReceivedAmount();
        completion.complete(locked.order(), locked.payment(), received, locked.log().getId(),
                locked.payment().getProviderTransactionId(), true, now);
        return record(admin, locked.log(), locked.payment(), ReconciliationActionType.APPROVE, before.name(),
                PaymentStatus.PAID.name(), reason, "PAYMENT_RECONCILIATION_APPROVED", now);
    }

    /**
     * Payment REVIEW_REQUIRED → FAILED. Với webhook UNMATCHED (không có Payment chờ duyệt), reject nghĩa là Admin
     * đã xử lý ngoài hệ thống, ví dụ hoàn tiền thủ công; webhook rời hàng đợi (Doc phần B, Mục 13 ý 8).
     */
    @Transactional
    public ReconciliationItemResponse reject(CurrentUser admin, long webhookId, String reason) {
        requireAdmin(admin);
        requireWebhook(webhookId);
        Instant now = clock.instant();
        Long paymentId = logs.findPaymentIdById(webhookId).orElse(null);
        if (paymentId != null) {
            Locked locked = lock(webhookId, paymentId);
            if (isReviewOf(locked.payment(), webhookId)) {
                PaymentStatus before = locked.payment().getStatus();
                locked.payment().markFailed(reason, now);
                return record(admin, locked.log(), locked.payment(), ReconciliationActionType.REJECT,
                        before.name(), PaymentStatus.FAILED.name(), reason, "PAYMENT_RECONCILIATION_REJECTED", now);
            }
            requireOpenUnmatched(locked.log());
            return closeUnmatched(admin, locked.log(), reason, now);
        }
        PaymentWebhookLog log = logs.findByIdForUpdate(webhookId).orElseThrow();
        requireOpenUnmatched(log);
        return closeUnmatched(admin, log, reason, now);
    }

    private ReconciliationItemResponse closeUnmatched(CurrentUser admin, PaymentWebhookLog log, String reason,
            Instant now) {
        String status = log.getProcessingStatus().name();
        return record(admin, log, null, ReconciliationActionType.REJECT, status, status, reason,
                "PAYMENT_WEBHOOK_RESOLVED_OUTSIDE", now);
    }

    /**
     * Ghép webhook UNMATCHED với một Payment (SHOULD) rồi áp lại bảng quyết định từ dòng 3: đúng tiền thì PAID,
     * lệch tiền hoặc Order hết hạn thì REVIEW_REQUIRED để Admin duyệt tiếp.
     */
    @Transactional
    public ReconciliationItemResponse match(CurrentUser admin, long webhookId, long paymentId, String reason) {
        requireAdmin(admin);
        Instant now = clock.instant();
        requireWebhook(webhookId);
        Locked locked = lock(webhookId, paymentId);
        Order order = locked.order();
        Payment payment = locked.payment();
        PaymentWebhookLog log = locked.log();
        requireOpenUnmatched(log);
        long amount = log.getReceivedAmount() == null ? 0 : log.getReceivedAmount();
        Outcome outcome = PaymentDecision.decide(new PaymentDecision.Input(payment.getPaymentReference(),
                payment.getStatus(), order.getStatus(), order.getExpiresAt(), payment.getExpectedAmount(), amount,
                log.getReceivedAt(), properties.lateGrace()));
        PaymentStatus before = payment.getStatus();
        switch (outcome.webhookStatus()) {
            case PROCESSED -> {
                completion.complete(order, payment, amount, log.getId(), log.getProviderTransactionId(), false, now);
                log.markProcessed(payment.getId(), now);
            }
            case MATCHED -> {
                if (outcome.expireOrderFirst()) {
                    order.expire(now);
                    payment.cancel(now);
                }
                payment.markReviewRequired(outcome.reviewReasonCode(), "Admin ghép thủ công: " + reason, amount,
                        log.getId(), log.getProviderTransactionId(), now);
                log.markMatchedForReview(payment.getId(), now);
            }
            default -> throw new BusinessException(ErrorCode.RECONCILIATION_INVALID,
                    "Payment " + paymentId + " is " + payment.getStatus() + " and cannot take this transfer");
        }
        return record(admin, log, payment, ReconciliationActionType.MATCH, before.name(),
                payment.getStatus().name(), reason, "PAYMENT_RECONCILIATION_MATCHED", now);
    }

    private record Locked(PaymentWebhookLog log, Order order, Payment payment) {
    }

    /** Khóa theo thứ tự Order → Payment → log và kiểm tra Payment đang chờ duyệt vì đúng webhook này. */
    private Locked lockReview(long webhookId) {
        requireWebhook(webhookId);
        Long paymentId = logs.findPaymentIdById(webhookId).orElseThrow(() -> new BusinessException(
                ErrorCode.PAYMENT_NOT_REVIEWABLE, "Webhook is not linked to a payment"));
        Locked locked = lock(webhookId, paymentId);
        if (!isReviewOf(locked.payment(), webhookId)) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_REVIEWABLE, "Payment " + paymentId + " is "
                    + locked.payment().getStatus() + ", not awaiting review for this webhook");
        }
        return locked;
    }

    /**
     * Chỉ đọc id bằng truy vấn trả giá trị đơn trước khi khóa: nếu nạp entity bằng truy vấn thường trước, truy vấn
     * {@code FOR UPDATE} sau sẽ trả lại đúng object cũ đó với dữ liệu có thể đã cũ.
     */
    private Locked lock(long webhookId, long paymentId) {
        Long orderId = payments.findOrderIdById(paymentId).orElseThrow(() -> BusinessException.notFound("Payment"));
        Order order = orders.findByIdForUpdate(orderId).orElseThrow();
        Payment payment = payments.findByIdForUpdate(paymentId).orElseThrow();
        PaymentWebhookLog log = logs.findByIdForUpdate(webhookId).orElseThrow();
        return new Locked(log, order, payment);
    }

    private static boolean isReviewOf(Payment payment, long webhookId) {
        return payment.getStatus() == PaymentStatus.REVIEW_REQUIRED
                && Long.valueOf(webhookId).equals(payment.getPaidWebhookLogId());
    }

    private void requireWebhook(long webhookId) {
        if (!logs.existsById(webhookId)) {
            throw BusinessException.notFound("Webhook");
        }
    }

    private void requireOpenUnmatched(PaymentWebhookLog log) {
        if (log.getProcessingStatus() != WebhookProcessingStatus.UNMATCHED
                || actions.existsByWebhookLogIdAndActionType(log.getId(), ReconciliationActionType.REJECT)) {
            throw new BusinessException(ErrorCode.RECONCILIATION_INVALID,
                    "Webhook " + log.getId() + " is not an open unmatched transfer");
        }
    }

    private ReconciliationItemResponse record(CurrentUser admin, PaymentWebhookLog log, Payment payment,
            ReconciliationActionType type, String previous, String next, String reason, String auditAction,
            Instant now) {
        PaymentReconciliationAction action = actions.save(new PaymentReconciliationAction(log.getId(),
                payment == null ? null : payment.getId(), payment == null ? null : payment.getOrderId(), admin.id(),
                type, previous, next, reason, now));
        audit.record(new AuditEntry(admin.id(), auditAction, payment == null ? "PaymentWebhookLog" : "Payment",
                payment == null ? log.getId() : payment.getId(), "{\"status\":\"" + previous + "\"}",
                "{\"status\":\"" + next + "\"}", reason));
        return ReconciliationItemResponse.from(log, payment, action);
    }

    private Page<ReconciliationItemResponse> withDetails(Page<PaymentWebhookLog> page) {
        List<Long> paymentIds = page.getContent().stream().map(PaymentWebhookLog::getPaymentId)
                .filter(id -> id != null).distinct().toList();
        Map<Long, Payment> paymentsById = payments.findAllById(paymentIds).stream()
                .collect(Collectors.toMap(Payment::getId, Function.identity()));
        Map<Long, PaymentReconciliationAction> lastActions = new HashMap<>();
        actions.findByWebhookLogIdInOrderByCreatedAtDescIdDesc(
                page.getContent().stream().map(PaymentWebhookLog::getId).toList())
                .forEach(a -> lastActions.putIfAbsent(a.getWebhookLogId(), a));
        return page.map(log -> ReconciliationItemResponse.from(log,
                log.getPaymentId() == null ? null : paymentsById.get(log.getPaymentId()), lastActions.get(log.getId())));
    }

    private static void requireAdmin(CurrentUser user) {
        if (!user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Administrator role required");
        }
    }
}
