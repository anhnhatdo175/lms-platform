package com.example.lms.ai;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * Các UPDATE {@code @Modifying} tự mở transaction ({@code @Transactional} trên từng method) vì
 * {@code DocumentIngestionService.process} cố ý chạy ngoài transaction để không giữ transaction quanh lời gọi HTTP.
 */
public interface AiKnowledgeDocumentRepository extends JpaRepository<AiKnowledgeDocument, Long> {

    Page<AiKnowledgeDocument> findByCourseIdAndDeletedAtIsNull(long courseId, Pageable pageable);

    /** Việc ingestion chờ xử lý: PENDING đến hạn, hoặc PROCESSING đã hết lease (thu hồi). */
    @Query("""
            SELECT d FROM AiKnowledgeDocument d
            WHERE d.deletedAt IS NULL
              AND ((d.indexStatus = com.example.lms.ai.IndexStatus.PENDING
                    AND (d.nextRetryAt IS NULL OR d.nextRetryAt <= :now))
                OR (d.indexStatus = com.example.lms.ai.IndexStatus.PROCESSING AND d.leaseExpiresAt <= :now))
            ORDER BY d.createdAt
            """)
    List<AiKnowledgeDocument> findRunnable(Instant now, Pageable pageable);

    /** Tài liệu truy xuất được trong cả Course (retrieval không giới hạn Lesson). */
    @Query("""
            SELECT d.id FROM AiKnowledgeDocument d
            WHERE d.courseId = :courseId AND d.deletedAt IS NULL
              AND d.indexStatus = com.example.lms.ai.IndexStatus.READY
            """)
    List<Long> findReadyIdsByCourse(long courseId);

    /** Tài liệu của Course ở mức Course (không gắn Lesson) hoặc đúng Lesson được hỏi. */
    @Query("""
            SELECT d.id FROM AiKnowledgeDocument d
            WHERE d.courseId = :courseId AND d.deletedAt IS NULL
              AND d.indexStatus = com.example.lms.ai.IndexStatus.READY
              AND (d.lessonId IS NULL OR d.lessonId = :lessonId)
            """)
    List<Long> findReadyIdsByCourseAndLesson(long courseId, long lessonId);

    // ---- ingestion: cùng quy tắc lease/fencing/retry như phiên (Spec 20A.14) ----

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AiKnowledgeDocument d
            SET d.indexStatus = com.example.lms.ai.IndexStatus.PROCESSING, d.attemptCount = d.attemptCount + 1,
                d.leaseExpiresAt = :lease, d.updatedAt = :now
            WHERE d.id = :id AND d.attemptCount = :seenAttempt AND d.deletedAt IS NULL
              AND (d.indexStatus = com.example.lms.ai.IndexStatus.PENDING
                   OR (d.indexStatus = com.example.lms.ai.IndexStatus.PROCESSING AND d.leaseExpiresAt <= :now))
            """)
    int claim(long id, int seenAttempt, Instant lease, Instant now);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AiKnowledgeDocument d
            SET d.indexStatus = com.example.lms.ai.IndexStatus.READY, d.leaseExpiresAt = NULL, d.nextRetryAt = NULL,
                d.errorMessage = NULL, d.updatedAt = :now
            WHERE d.id = :id AND d.indexStatus = com.example.lms.ai.IndexStatus.PROCESSING
              AND d.attemptCount = :attempt AND d.deletedAt IS NULL
            """)
    int markReady(long id, int attempt, Instant now);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AiKnowledgeDocument d
            SET d.indexStatus = com.example.lms.ai.IndexStatus.FAILED, d.leaseExpiresAt = NULL, d.nextRetryAt = NULL,
                d.errorMessage = :message, d.updatedAt = :now
            WHERE d.id = :id AND d.indexStatus = com.example.lms.ai.IndexStatus.PROCESSING AND d.attemptCount = :attempt
            """)
    int markFailed(long id, int attempt, String message, Instant now);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AiKnowledgeDocument d
            SET d.indexStatus = com.example.lms.ai.IndexStatus.PENDING, d.leaseExpiresAt = NULL, d.nextRetryAt = :next,
                d.errorMessage = :message, d.updatedAt = :now
            WHERE d.id = :id AND d.indexStatus = com.example.lms.ai.IndexStatus.PROCESSING AND d.attemptCount = :attempt
            """)
    int markRequeued(long id, int attempt, Instant next, String message, Instant now);

    /** Lưu văn bản đã trích xuất để reindex không phải đọc lại tệp; fenced theo attempt. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AiKnowledgeDocument d SET d.sourceText = :text, d.updatedAt = :now
            WHERE d.id = :id AND d.indexStatus = com.example.lms.ai.IndexStatus.PROCESSING AND d.attemptCount = :attempt
            """)
    int storeExtractedText(long id, int attempt, String text, Instant now);

    boolean existsByCourseIdAndContentHashAndDeletedAtIsNull(Long courseId, String contentHash);

    /** Danh sách chưa xóa, lọc tùy chọn theo course/lesson/trạng thái (Teacher luôn truyền courseId đã kiểm quyền). */
    @Query("""
            SELECT d FROM AiKnowledgeDocument d
            WHERE d.deletedAt IS NULL
              AND (:courseId IS NULL OR d.courseId = :courseId)
              AND (:lessonId IS NULL OR d.lessonId = :lessonId)
              AND (:status IS NULL OR d.indexStatus = :status)
            """)
    Page<AiKnowledgeDocument> search(Long courseId, Long lessonId, IndexStatus status, Pageable pageable);

    /** Index lại: tăng index_version, về PENDING. Không áp dụng khi đang PROCESSING hoặc đã xóa. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE AiKnowledgeDocument d
            SET d.indexStatus = com.example.lms.ai.IndexStatus.PENDING, d.indexVersion = d.indexVersion + 1,
                d.attemptCount = 0, d.leaseExpiresAt = NULL, d.nextRetryAt = NULL, d.errorMessage = NULL,
                d.updatedAt = :now
            WHERE d.id = :id AND d.deletedAt IS NULL
              AND d.indexStatus <> com.example.lms.ai.IndexStatus.PROCESSING
            """)
    int markReindex(long id, Instant now);
}
