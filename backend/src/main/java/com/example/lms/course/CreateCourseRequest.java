package com.example.lms.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String slug,
        @Size(max = 500) String shortDescription,
        String description,
        @NotNull @Positive Long categoryId,
        @NotNull @PositiveOrZero Long originalPrice,
        @NotNull @PositiveOrZero Long salePrice,
        Long thumbnailResourceId,
        @Size(max = 100) String subjectCode,
        @Size(max = 50) String gradeLevel,
        @Positive Integer accessDurationDays,
        CourseCompletionRule completionRule) {
}

