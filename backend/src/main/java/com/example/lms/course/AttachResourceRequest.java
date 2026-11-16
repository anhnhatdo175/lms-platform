package com.example.lms.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttachResourceRequest(
        @NotNull @Min(1) Long resourceId,
        @Size(max = 255) String title,
        LessonResourceType resourceType,
        Boolean downloadable,
        @Min(1) Integer position) {
}
