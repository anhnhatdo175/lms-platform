package com.example.lms.audit;

import java.time.Instant;
import java.util.Map;

public record AuditLogEntry(
        Long id,
        Long actorUserId,
        String actorEmail,
        String action,
        String entityType,
        Long entityId,
        String ipAddress,
        Instant createdAt,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        String reason) {

    public static AuditLogEntry from(AuditLog log) {
        return new AuditLogEntry(
                log.getId(),
                log.getActorUserId(),
                log.getActorEmail(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getIpAddress(),
                log.getCreatedAt(),
                log.getOldValue(),
                log.getNewValue(),
                log.getReason());
    }
}
