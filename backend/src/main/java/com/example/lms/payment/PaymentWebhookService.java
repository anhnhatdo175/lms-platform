package com.example.lms.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.lms.commerce.Order;
import com.example.lms.commerce.OrderRepository;
import com.example.lms.payment.PaymentDecision.Outcome;
import com.example.lms.payment.provider.SepayApiKeyVerifier;
import com.example.lms.payment.provider.SepayWebhookPayload;

/**
 * Nhận webhook SePay (Spec 11.3–11.8). Ghi log trong transaction riêng trước, rồi mới xử lý nghiệp vụ trong
 * transaction thứ hai, để không mất dấu request khi transaction nghiệp vụ rollback (Spec 11.7).
 */
@Service
public class PaymentWebhookService {

    public enum Result {
        /** Trả 200 {@code {"success": true}}: đã nhận, kể cả trùng, không khớp hay cần đối soát. */
        ACCEPTED,
        /** Trả 401: sai API key. */
        UNAUTHORIZED
    }

    private final PaymentWebhookLogRepository logs;
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final PaymentCompletionService completion;
    private final PaymentReferenceGenerator references;
    private final SepayApiKeyVerifier verifier;
    private final PaymentProperties properties;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public PaymentWebhookService(PaymentWebhookLogRepository logs, PaymentRepository payments, OrderRepository orders,
            PaymentCompletionService completion, PaymentReferenceGenerator references, SepayApiKeyVerifier verifier,
            PaymentProperties properties, TransactionTemplate transactions, Clock clock) {
        this.logs = logs;
        this.payments = payments;
        this.orders = orders;
        this.completion = completion;
        this.references = references;
        this.verifier = verifier;
        this.properties = properties;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * Lỗi hệ thống (database...) được ném ra để controller trả 500 và SePay gửi lại; log gốc đã commit ở trạng
     * thái RECEIVED nên lần gửi lại sẽ xử lý tiếp thay vì bị coi là trùng rồi bỏ qua.
     */
    public Result receive(String authorizationHeader, String rawBody) {
        Instant now = clock.instant();
        String payload = rawBody == null ? "" : rawBody;
        if (!verifier.verify(authorizationHeader)) {
            save(PaymentWebhookLog.unauthenticated(PaymentProviderType.SEPAY, payload, "SEPAY:unauthenticated", now));
            return Result.UNAUTHORIZED;
        }

        SepayWebhookPayload data;
        try {
            data = SepayWebhookPayload.parse(payload);
        } catch (IllegalArgumentException malformed) {
            save(PaymentWebhookLog.ignored(PaymentProviderType.SEPAY, null, null, null, payload,
                    "SEPAY:malformed:" + sha256(payload), "Malformed payload: " + malformed.getMessage(), now));
            return Result.ACCEPTED;
        }
        String reference = references.extract(data.code(), data.content()).orElse(null);

        // Lọc trước bảng quyết định: không phải tiền vào tài khoản của hệ thống (Doc phần B, Mục 13 ý 6)
        String ignoredReason = ignoredReason(data);
        if (ignoredReason != null) {
            save(PaymentWebhookLog.ignored(PaymentProviderType.SEPAY, data.transactionId(), reference,
                    data.transferAmount(), payload, data.dedupKey(), ignoredReason, now));
            return Result.ACCEPTED;
        }

        Long primaryLogId;
        try {
            primaryLogId = transactions.execute(status -> logs.saveAndFlush(PaymentWebhookLog.firstReceipt(
                    PaymentProviderType.SEPAY, null, data.transactionId(), reference, data.transferAmount(), payload,
                    data.dedupKey(), now)).getId());
        } catch (DataIntegrityViolationException alreadyReceived) {
            // UNIQUE(primary_dedup_key) đã chốt: đây là bản lặp (AC-PAY-01, AC-PAY-02)
            save(PaymentWebhookLog.duplicateOf(PaymentProviderType.SEPAY, null, data.transactionId(), reference,
                    data.transferAmount(), payload, data.dedupKey(), now));
            primaryLogId = transactions.execute(status -> logs.findIdByPrimaryDedupKey(data.dedupKey()).orElse(null));
        }
        if (primaryLogId != null) {
            // Với bản lặp, process() không làm gì nếu bản gốc đã xử lý xong; còn RECEIVED thì xử lý thay
            process(primaryLogId);
        }
        return Result.ACCEPTED;
    }

    /**
     * Áp bảng quyết định cho một log gốc; bỏ qua nếu log đã xử lý xong. Dùng chung cho request, bản lặp và job
     * quét log kẹt. Thứ tự khóa Order → Payment → log, giống mọi luồng tiền khác.
     */
    public void process(long logId) {
        transactions.executeWithoutResult(status -> {
            String reference = logs.findReferenceById(logId).orElse(null);
            Optional<Long> orderId = reference == null ? Optional.empty()
                    : payments.findOrderIdByPaymentReference(reference);
            Order order = orderId.flatMap(orders::findByIdForUpdate).orElse(null);
            Payment payment = order == null ? null : payments.findByPaymentReferenceForUpdate(reference).orElse(null);
            PaymentWebhookLog log = logs.findByIdForUpdate(logId).orElseThrow();
            if (log.getProcessingStatus().isFinished()) {
                return;
            }
            long amount = log.getReceivedAmount() == null ? 0 : log.getReceivedAmount();
            // Thời điểm nhận ban đầu, không phải lúc xử lý lại, quyết định còn trong ân hạn hay không
            Outcome outcome = PaymentDecision.decide(new PaymentDecision.Input(reference,
                    payment == null ? null : payment.getStatus(), order == null ? null : order.getStatus(),
                    order == null ? null : order.getExpiresAt(), payment == null ? 0 : payment.getExpectedAmount(),
                    amount, log.getReceivedAt(), properties.lateGrace()));
            apply(outcome, log, order, payment, amount, clock.instant());
        });
    }

    private void apply(Outcome outcome, PaymentWebhookLog log, Order order, Payment payment, long amount,
            Instant now) {
        if (outcome.expireOrderFirst()) {
            order.expire(now);
            payment.cancel(now);
        }
        switch (outcome.webhookStatus()) {
            case UNMATCHED -> log.markUnmatched(outcome.unmatchedReason(), payment == null ? null : payment.getId(),
                    now);
            case MATCHED -> {
                payment.markReviewRequired(outcome.reviewReasonCode(), describe(outcome, order, payment, amount),
                        amount, log.getId(), log.getProviderTransactionId(), now);
                log.markMatchedForReview(payment.getId(), now);
            }
            case PROCESSED -> {
                completion.complete(order, payment, amount, log.getId(), log.getProviderTransactionId(), false, now);
                log.markProcessed(payment.getId(), now);
            }
            default -> throw new IllegalStateException("Unexpected outcome " + outcome);
        }
    }

    private String ignoredReason(SepayWebhookPayload data) {
        if (!data.isIncoming()) {
            return "Outgoing transfer ignored";
        }
        String account = properties.sepay().accountNumber();
        if (account != null && !account.isBlank() && !account.equals(data.accountNumber())) {
            return "Transfer to another bank account ignored";
        }
        return null;
    }

    private static String describe(Outcome outcome, Order order, Payment payment, long amount) {
        return switch (outcome.reviewReasonCode()) {
            case ORDER_NOT_PAYABLE -> "Nhận " + amount + " đ khi đơn " + order.getCode() + " đã " + order.getStatus();
            case AMOUNT_UNDERPAID -> "Nhận " + amount + " đ, cần " + payment.getExpectedAmount() + " đ (thiếu)";
            case AMOUNT_OVERPAID -> "Nhận " + amount + " đ, cần " + payment.getExpectedAmount() + " đ (thừa)";
        };
    }

    private void save(PaymentWebhookLog log) {
        transactions.executeWithoutResult(status -> logs.save(log));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
