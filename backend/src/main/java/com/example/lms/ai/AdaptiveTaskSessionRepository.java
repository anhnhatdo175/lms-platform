package com.example.lms.ai;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdaptiveTaskSessionRepository extends JpaRepository<AdaptiveTaskSession, Long> {

    Optional<AdaptiveTaskSession> findByStudentIdAndClientRequestId(long studentId, String clientRequestId);

    Page<AdaptiveTaskSession> findByStudentId(long studentId, Pageable pageable);
}
