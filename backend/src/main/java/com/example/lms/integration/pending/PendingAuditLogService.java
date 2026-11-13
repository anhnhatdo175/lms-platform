package com.example.lms.integration.pending;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.lms.contract.audit.AuditLogService;

/** BẢN TẠM của B để ứng dụng khởi động được khi A chưa có bảng {@code audit_logs}: ghi tạm ra log ứng dụng. A thay. */
@Component
public class PendingAuditLogService implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(PendingAuditLogService.class);

    @Override
    public void record(AuditEntry entry) {
        log.info("AUDIT actor={} action={} entity={}#{} reason={} old={} new={}", entry.actorUserId(),
                entry.action(), entry.entityType(), entry.entityId(), entry.reason(), entry.oldValueJson(),
                entry.newValueJson());
    }
}
