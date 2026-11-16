package com.example.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, CourseTeacherId> {
    boolean existsByIdCourseIdAndIdTeacherIdAndActiveTrue(Long courseId, Long teacherId);

    List<CourseTeacher> findAllByIdCourseIdAndActiveTrueOrderByPrimaryDescIdTeacherIdAsc(Long courseId);

    List<CourseTeacher> findAllByIdCourseIdOrderByIdTeacherIdAsc(Long courseId);
}
