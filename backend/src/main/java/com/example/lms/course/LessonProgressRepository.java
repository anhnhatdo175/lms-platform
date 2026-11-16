package com.example.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByStudentIdAndCourseIdAndLessonId(
            long studentId, long courseId, long lessonId);

    List<LessonProgress> findAllByStudentIdAndCourseId(long studentId, long courseId);

    List<LessonProgress> findAllByStudentIdAndCourseIdIn(long studentId, Collection<Long> courseIds);

    boolean existsByStudentIdAndLessonIdAndStatus(
            long studentId, long lessonId, LessonProgressStatus status);
}
