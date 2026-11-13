package com.example.lms.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsBySlug(String slug);

    Optional<Course> findBySlugAndStatus(String slug, CourseStatus status);

    @Query("""
            select course from Course course
            where course.id = :courseId
              and exists (
                select assignment.id from CourseTeacher assignment
                where assignment.course = course
                  and assignment.teacher.id = :teacherId
                  and assignment.active = true)
            """)
    Optional<Course> findTeacherCourse(
            @Param("courseId") Long courseId,
            @Param("teacherId") Long teacherId);

    @Query("""
            select course from Course course
            where (:status is null or course.status = :status)
              and (:query is null or lower(course.title) like lower(concat('%', :query, '%')))
              and exists (
                select assignment.id from CourseTeacher assignment
                where assignment.course = course
                  and assignment.teacher.id = :teacherId
                  and assignment.active = true)
            """)
    Page<Course> searchTeacherCourses(
            @Param("teacherId") Long teacherId,
            @Param("status") CourseStatus status,
            @Param("query") String query,
            Pageable pageable);

    @Query("""
            select course from Course course
            where course.status = com.example.lms.course.CourseStatus.PUBLISHED
              and (:categoryId is null or course.category.id = :categoryId)
              and (:query is null or lower(course.title) like lower(concat('%', :query, '%')))
            """)
    Page<Course> searchPublicCourses(
            @Param("categoryId") Long categoryId,
            @Param("query") String query,
            Pageable pageable);
}
