package com.example.lms.course;

import java.time.Instant;

public record SectionSummary(
        Long id,
        Long courseId,
        String title,
        String description,
        int position,
        Instant createdAt,
        Instant updatedAt,
        boolean published,
        int releaseAfterDays) {

    public static SectionSummary from(CourseSection section) {
        return new SectionSummary(
                section.getId(),
                section.getCourse().getId(),
                section.getTitle(),
                section.getDescription(),
                section.getPosition(),
                section.getCreatedAt(),
                section.getUpdatedAt(),
                section.isPublished(),
                section.getReleaseAfterDays());
    }
}
