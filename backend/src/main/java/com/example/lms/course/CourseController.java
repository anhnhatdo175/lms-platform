package com.example.lms.course;

import com.example.lms.auth.UserPrincipal;
import com.example.lms.common.ApiResponse;
import com.example.lms.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import tools.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/teacher/courses")
@PreAuthorize("hasRole('TEACHER')")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseManaged> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCourseRequest request) {
        return new ApiResponse<>(courseService.create(principal, request));
    }

    @GetMapping
    public PageResponse<CourseManaged> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort) {
        return courseService.listTeacherCourses(principal, status, q, page, size, sort);
    }

    @GetMapping("/{courseId}")
    public ApiResponse<CourseManaged> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId) {
        return new ApiResponse<>(courseService.getTeacherCourse(principal, courseId));
    }

    @PatchMapping("/{courseId}")
    public ApiResponse<CourseManaged> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId,
            @RequestBody JsonNode changes) {
        return new ApiResponse<>(courseService.update(principal, courseId, changes));
    }
}
