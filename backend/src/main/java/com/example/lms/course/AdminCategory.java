package com.example.lms.course;

import java.time.Instant;

public record AdminCategory(
        Long id,
        String name,
        String slug,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminCategory from(CourseCategory category) {
        return new AdminCategory(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
