package com.example.lms.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CurriculumOrderRequest(@NotEmpty List<@Valid SectionOrder> sections) {

    public record SectionOrder(
            @NotNull @Min(1) Long sectionId,
            @NotNull List<@NotNull @Min(1) Long> lessonIds) {
    }
}
