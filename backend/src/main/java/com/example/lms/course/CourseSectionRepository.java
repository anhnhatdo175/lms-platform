package com.example.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseSectionRepository extends JpaRepository<CourseSection, Long> {

    @Query("select coalesce(max(section.position), 0) from CourseSection section where section.course.id = :courseId")
    int findLastPosition(@Param("courseId") Long courseId);

    boolean existsByCourse_IdAndPosition(Long courseId, int position);

    long countByCourse_Id(Long courseId);

    Optional<CourseSection> findByIdAndCourse_Id(Long sectionId, Long courseId);
}
