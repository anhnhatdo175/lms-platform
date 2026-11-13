package com.example.lms.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSectionRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 65535) String description,
        @Min(1) Integer position,
        Boolean published,
        @Min(0) Integer releaseAfterDays) {
}
