package com.example.lms.exam;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findByLessonId(long lessonId);
}
