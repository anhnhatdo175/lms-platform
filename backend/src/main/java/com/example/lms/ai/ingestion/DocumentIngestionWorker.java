package com.example.lms.ai.ingestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DocumentIngestionWorker {

    private final DocumentIngestionService service;
    private final boolean enabled;

    public DocumentIngestionWorker(DocumentIngestionService service,
            @Value("${ai.ingestion.worker-enabled:true}") boolean enabled) {
        this.service = service;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${ai.ingestion.worker-delay:PT3S}")
    public void tick() {
        if (!enabled) {
            return;
        }
        // claim() và process() gọi qua proxy Spring: claim chạy trong transaction, process thì không.
        for (int i = 0; i < 20; i++) {
            var claimed = service.claim();
            if (claimed.isEmpty()) {
                return;
            }
            claimed.forEach(service::process);
        }
    }
}
