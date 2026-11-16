package com.example.lms.course;

import com.example.lms.common.ApiResponse;
import com.example.lms.common.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/courses")
public class CourseCatalogController {

    private final CourseCatalogService catalogService;

    public CourseCatalogController(CourseCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public PageResponse<CourseSummary> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @Min(1) Long categoryId,
            @RequestParam(required = false) @Min(0) Long minPrice,
            @RequestParam(required = false) @Min(0) Long maxPrice,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "newest") String sort) {
        return catalogService.list(q, categoryId, minPrice, maxPrice, page, size, sort);
    }

    @GetMapping("/{slug}")
    public ApiResponse<CourseDetail> get(@PathVariable String slug) {
        return new ApiResponse<>(catalogService.get(slug));
    }
}
