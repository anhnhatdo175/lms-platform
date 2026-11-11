package com.example.lms.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select log from AuditLog log
            where (:actorUserId is null or log.actorUserId = :actorUserId)
              and (:action is null or log.action = :action)
              and (:entityType is null or log.entityType = :entityType)
              and (:from is null or log.createdAt >= :from)
              and (:to is null or log.createdAt < :to)
            """)
    Page<AuditLog> search(
            @Param("actorUserId") Long actorUserId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
