package com.example.lms.ai;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface LearningInteractionRepository extends JpaRepository<LearningInteraction, Long> {

    boolean existsByEventKey(String eventKey);

    /** Tương tác chưa dùng để cập nhật hồ sơ, theo thứ tự thời gian (job xử lý lại, ERD 17.8). */
    @Query("SELECT i.id FROM LearningInteraction i WHERE i.processedAt IS NULL ORDER BY i.occurredAt, i.id")
    List<Long> findPendingIds(Pageable pageable);

    /** Đánh dấu đã áp dụng; 0 dòng nghĩa là luồng khác đã áp dụng trước (chống áp dụng EMA hai lần). */
    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("UPDATE LearningInteraction i SET i.processedAt = :now WHERE i.id = :id AND i.processedAt IS NULL")
    int markProcessed(long id, Instant now);

    List<LearningInteraction> findByStudentIdOrderByOccurredAtDesc(long studentId, Pageable pageable);

    /**
     * Attempt đã chấm gần đây mà chưa có tương tác nào (recorder bị lỗi/bỏ lỡ): job quét để ghi bù (Interface 6).
     * Attempt không có câu nào đã trả lời không tạo tương tác, nên chỉ được quét lại trong cửa sổ {@code since}.
     */
    @Query("""
            SELECT a.id FROM ExamAttempt a
            WHERE a.status = com.example.lms.exam.AttemptStatus.GRADED AND a.gradedAt >= :since
              AND NOT EXISTS (SELECT 1 FROM LearningInteraction i WHERE i.sourceAttemptId = a.id)
            ORDER BY a.gradedAt DESC
            """)
    List<Long> findGradedAttemptIdsWithoutInteractions(Instant since, Pageable pageable);

    /** Kỹ năng học viên đã có tương tác trong một Course (phạm vi Teacher được xem). */
    @Query("""
            SELECT DISTINCT i.skillCode FROM LearningInteraction i
            WHERE i.studentId = :studentId AND i.courseId = :courseId AND i.skillCode IS NOT NULL
            """)
    List<String> findSkillCodesByStudentAndCourse(long studentId, long courseId);
}
