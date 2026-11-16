package com.example.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonPrerequisiteRepository extends JpaRepository<LessonPrerequisite, Long> {

    Optional<LessonPrerequisite> findByLesson_Id(Long lessonId);

    List<LessonPrerequisite> findAllByConditionType(PrerequisiteConditionType conditionType);

    void deleteByLesson_Id(Long lessonId);
}
