package com.example.lms.contract.audit;

/**
 * Interface M3, A cung cấp (bảng {@code audit_logs}). B ghi khi Admin đối soát, grant/revoke Enrollment,
 * và khi Payment đổi trạng thái thủ công (Spec 16). Ghi trong cùng transaction với thay đổi nghiệp vụ.
 */
public interface AuditLogService {

    void record(AuditEntry entry);

    /**
     * @param actorUserId {@code null} nếu do hệ thống
     * @param oldValueJson, newValueJson JSON hoặc {@code null}
     */
    record AuditEntry(Long actorUserId, String action, String entityType, Long entityId, String oldValueJson,
            String newValueJson, String reason) {
    }
}
