package com.example.lms.ai.session;

import java.time.Clock;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Worker định kỳ xử lý phiên PENDING. Tắt bằng {@code ai.session.worker-enabled=false} (ví dụ khi chạy nhiều bản backend
 * mà chỉ muốn một bản xử lý; nhiều worker đồng thời vẫn an toàn nhờ lease + fencing).
 */
@Component
public class AdaptiveSessionWorker {

    private static final int MAX_PER_TICK = 5;

    private final SessionProcessor processor;
    private final boolean enabled;

    public AdaptiveSessionWorker(SessionStore store, SessionTaskProducer producer, Clock clock,
            @Value("${ai.session.worker-enabled:true}") boolean enabled) {
        this.processor = new SessionProcessor(store, producer, clock, "worker-" + UUID.randomUUID());
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${ai.session.worker-delay:PT2S}")
    public void tick() {
        if (!enabled) {
            return;
        }
        for (int i = 0; i < MAX_PER_TICK && processor.runOnce(); i++) {
            // drain
        }
    }
}
