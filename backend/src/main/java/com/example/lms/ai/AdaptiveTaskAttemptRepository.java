package com.example.lms.ai;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdaptiveTaskAttemptRepository extends JpaRepository<AdaptiveTaskAttempt, Long> {

    Optional<AdaptiveTaskAttempt> findByStudentIdAndClientEventId(long studentId, String clientEventId);

    Optional<AdaptiveTaskAttempt> findByTaskIdAndStudentId(long taskId, long studentId);

    Page<AdaptiveTaskAttempt> findByStudentId(long studentId, Pageable pageable);
}
