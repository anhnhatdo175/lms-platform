package com.example.lms.ai;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdaptivePracticeConfigRepository extends JpaRepository<AdaptivePracticeConfig, Long> {

    /** Cấu hình mặc định của Course (lesson_id IS NULL). */
    Optional<AdaptivePracticeConfig> findByCourseIdAndLessonIdIsNull(long courseId);

    Optional<AdaptivePracticeConfig> findByCourseIdAndLessonId(long courseId, long lessonId);

    List<AdaptivePracticeConfig> findByCourseId(long courseId);
}
