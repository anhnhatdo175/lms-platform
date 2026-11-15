package com.example.lms.ai;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
