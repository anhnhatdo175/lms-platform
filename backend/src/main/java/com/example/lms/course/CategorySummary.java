package com.example.lms.course;

public record CategorySummary(Long id, String name, String slug) {
    public static CategorySummary from(CourseCategory category) {
        return new CategorySummary(category.getId(), category.getName(), category.getSlug());
    }
}
