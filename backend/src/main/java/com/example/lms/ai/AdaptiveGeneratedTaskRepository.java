package com.example.lms.ai;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdaptiveGeneratedTaskRepository extends JpaRepository<AdaptiveGeneratedTask, Long> {

    List<AdaptiveGeneratedTask> findBySessionIdOrderByPosition(long sessionId);

    long countBySessionId(long sessionId);
}
