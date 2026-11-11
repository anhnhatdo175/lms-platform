package com.example.lms.audit;

import com.example.lms.auth.AuthException;
import com.example.lms.common.PageMeta;
import com.example.lms.common.PageResponse;
import com.example.lms.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(
            User actor,
            String action,
            String entityType,
            Long entityId,
            String ipAddress,
            String userAgent,
            Map<String, Object> oldValue,
            Map<String, Object> newValue,
            String reason) {
        auditLogRepository.save(new AuditLog(
                actor,
                action,
                entityType,
                entityId,
                ipAddress,
                userAgent,
                oldValue,
                newValue,
                reason));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogEntry> list(
            Long actorUserId,
            String action,
            String entityType,
            Instant from,
            Instant to,
            int page,
            int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new AuthException("INVALID_DATE_RANGE", "Khoảng thời gian không hợp lệ.", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        Page<AuditLog> logs = auditLogRepository.search(
                actorUserId,
                blankToNull(action),
                blankToNull(entityType),
                from,
                to,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(
                        Sort.by(Sort.Direction.DESC, "id"))));
        return new PageResponse<>(
                logs.map(AuditLogEntry::from).getContent(),
                new PageMeta(logs.getNumber(), logs.getSize(), logs.getTotalElements(), logs.getTotalPages()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
