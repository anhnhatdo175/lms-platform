package com.example.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {
    boolean existsBySlug(String slug);

    Optional<CourseCategory> findBySlug(String slug);

    List<CourseCategory> findAllByActiveTrueOrderByNameAsc();
}
