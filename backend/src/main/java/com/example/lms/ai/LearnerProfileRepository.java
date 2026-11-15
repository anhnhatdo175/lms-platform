package com.example.lms.ai;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LearnerProfileRepository extends JpaRepository<LearnerProfile, Long> {

    Optional<LearnerProfile> findByStudentId(long studentId);
}
