package com.example.lms.ai;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningInteractionRepository extends JpaRepository<LearningInteraction, Long> {

    boolean existsByEventKey(String eventKey);

    /** Tương tác chưa dùng để cập nhật hồ sơ, theo thứ tự thời gian (job xử lý lại, ERD 17.8). */
    List<LearningInteraction> findByProcessedAtIsNullOrderByOccurredAt(Pageable pageable);

    List<LearningInteraction> findByStudentIdOrderByOccurredAtDesc(long studentId, Pageable pageable);
}
