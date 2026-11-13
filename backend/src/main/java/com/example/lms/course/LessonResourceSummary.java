package com.example.lms.course;

public record LessonResourceSummary(
        Long id,
        Long resourceId,
        String title,
        LessonResourceType resourceType,
        boolean downloadable,
        String url,
        int position) {
}
