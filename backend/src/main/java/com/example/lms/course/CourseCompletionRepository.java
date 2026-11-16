package com.example.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseCompletionRepository extends JpaRepository<CourseCompletion, Long> {

    Optional<CourseCompletion> findByStudentIdAndCourseId(long studentId, long courseId);
}
