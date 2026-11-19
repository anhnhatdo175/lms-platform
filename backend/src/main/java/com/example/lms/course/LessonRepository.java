package com.example.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("select coalesce(max(lesson.position), 0) from Lesson lesson where lesson.section.id = :sectionId")
    int findLastPosition(@Param("sectionId") Long sectionId);

    boolean existsBySection_IdAndPosition(Long sectionId, int position);

    long countBySection_Id(Long sectionId);

    Optional<Lesson> findByIdAndSection_Id(Long lessonId, Long sectionId);

    boolean existsBySection_Course_IdAndPublishedTrueAndMandatoryTrue(Long courseId);

    List<Lesson> findAllBySection_IdAndPublishedTrueOrderByPositionAscIdAsc(Long sectionId);

    List<Lesson> findAllBySection_IdOrderByPositionAscIdAsc(Long sectionId);

    @Query("""
            select lesson from Lesson lesson
            where lesson.section.course.id = :courseId
            order by lesson.section.position, lesson.position, lesson.id
            """)
    List<Lesson> findAllByCourseIdOrderBySectionAndPosition(@Param("courseId") Long courseId);
}
