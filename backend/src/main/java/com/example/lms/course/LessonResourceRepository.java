package com.example.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonResourceRepository extends JpaRepository<LessonResource, Long> {
    List<LessonResource> findAllByLesson_IdOrderByPositionAscIdAsc(Long lessonId);

    Optional<LessonResource> findByLesson_IdAndResourceId(Long lessonId, Long resourceId);

    boolean existsByLesson_IdAndResourceId(Long lessonId, Long resourceId);

    boolean existsByLesson_IdAndPosition(Long lessonId, int position);

    int countByLesson_Id(Long lessonId);
}
