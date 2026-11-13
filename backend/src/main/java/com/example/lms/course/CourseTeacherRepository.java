package com.example.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, CourseTeacherId> {
    boolean existsByIdCourseIdAndIdTeacherIdAndActiveTrue(Long courseId, Long teacherId);
}
