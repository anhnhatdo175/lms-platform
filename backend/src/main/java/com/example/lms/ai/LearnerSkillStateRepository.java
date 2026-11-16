package com.example.lms.ai;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LearnerSkillStateRepository extends JpaRepository<LearnerSkillState, Long> {

    List<LearnerSkillState> findByStudentId(long studentId);

    Optional<LearnerSkillState> findByStudentIdAndSkillCode(long studentId, String skillCode);
}
