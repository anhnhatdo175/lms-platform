package com.example.lms.ai;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdaptiveTaskHintEventRepository extends JpaRepository<AdaptiveTaskHintEvent, Long> {

    Optional<AdaptiveTaskHintEvent> findByStudentIdAndIdempotencyKey(long studentId, String idempotencyKey);

    long countByTaskIdAndStudentId(long taskId, long studentId);
}
