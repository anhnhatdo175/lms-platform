package com.example.lms.ai;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LearningInteractionRepository extends JpaRepository<LearningInteraction, Long> {

    boolean existsByEventKey(String eventKey);

    /** Tương tác chưa dùng để cập nhật hồ sơ, theo thứ tự thời gian (job xử lý lại, ERD 17.8). */
    List<LearningInteraction> findByProcessedAtIsNullOrderByOccurredAt(Pageable pageable);

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
    List<Long> findGradedAttemptIdsWithoutInteractions(java.time.Instant since, Pageable pageable);

    /** Kỹ năng học viên đã có tương tác trong một Course (phạm vi Teacher được xem). */
    @Query("""
            SELECT DISTINCT i.skillCode FROM LearningInteraction i
            WHERE i.studentId = :studentId AND i.courseId = :courseId AND i.skillCode IS NOT NULL
            """)
    List<String> findSkillCodesByStudentAndCourse(long studentId, long courseId);
}
