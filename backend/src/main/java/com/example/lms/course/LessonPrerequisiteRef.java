package com.example.lms.course;

public record LessonPrerequisiteRef(Long lessonId, Long examId, String conditionType) {
}
