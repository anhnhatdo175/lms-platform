package com.example.lms.course;

import com.example.lms.common.ApiResponse;
import com.example.lms.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1")
public class CourseCategoryController {

    private final CourseCategoryService categoryService;

    public CourseCategoryController(CourseCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategorySummary>> listActive() {
        return new ApiResponse<>(categoryService.listActive());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/categories")
    public PageResponse<AdminCategory> listAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return categoryService.listAll(page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminCategory> create(@Valid @RequestBody CreateCategoryRequest request) {
        return new ApiResponse<>(categoryService.create(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/categories/{categoryId}")
    public ApiResponse<AdminCategory> update(
            @PathVariable @Min(1) Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return new ApiResponse<>(categoryService.update(categoryId, request));
    }
}
