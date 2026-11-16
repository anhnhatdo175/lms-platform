package com.example.lms.course;

public record LessonResourceSummary(
        Long id,
        Long resourceId,
        String title,
        LessonResourceType resourceType,
        boolean downloadable,
        String url,
        int position) {

    public static LessonResourceSummary from(LessonResource resource, String url) {
        return new LessonResourceSummary(
                resource.getId(),
                resource.getResourceId(),
                resource.getTitle(),
                resource.getResourceType(),
                resource.isDownloadable(),
                url,
                resource.getPosition());
    }
}
