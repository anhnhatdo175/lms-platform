package com.example.lms.payment;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Xử lý lại log gốc kẹt ở RECEIVED hơn 5 phút: lần xử lý trước lỗi mà SePay không gửi lại nữa.
 * {@link PaymentWebhookService#process} bỏ qua log đã xong, nên chạy trùng với request thật cũng an toàn.
 */
@Component
public class StuckWebhookJob {

    private static final Logger log = LoggerFactory.getLogger(StuckWebhookJob.class);
    private static final Duration STUCK_AFTER = Duration.ofMinutes(5);

    private final PaymentWebhookLogRepository logs;
    private final PaymentWebhookService webhooks;
    private final Clock clock;

    public StuckWebhookJob(PaymentWebhookLogRepository logs, PaymentWebhookService webhooks, Clock clock) {
        this.logs = logs;
        this.webhooks = webhooks;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${payment.stuck-webhook-job-delay:PT5M}")
    public void reprocessStuckLogs() {
        List<Long> ids = logs.findStuckIds(
                List.of(WebhookProcessingStatus.RECEIVED, WebhookProcessingStatus.VALIDATED),
                clock.instant().minus(STUCK_AFTER), PageRequest.of(0, 100));
        for (Long id : ids) {
            try {
                webhooks.process(id);
            } catch (RuntimeException e) {
                log.warn("Webhook log {} is still failing", id, e);
            }
        }
    }
}
