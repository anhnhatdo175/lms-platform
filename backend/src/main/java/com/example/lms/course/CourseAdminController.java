package com.example.lms.course;

import com.example.lms.auth.UserPrincipal;
import com.example.lms.common.ApiResponse;
import com.example.lms.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/admin/courses")
@PreAuthorize("hasRole('ADMIN')")
public class CourseAdminController {

    private final CourseAdminService courseAdminService;

    public CourseAdminController(CourseAdminService courseAdminService) {
        this.courseAdminService = courseAdminService;
    }

    @GetMapping
    public PageResponse<CourseManaged> list(
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(required = false) @Min(1) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return courseAdminService.list(status, categoryId, q, page, size);
    }

    @PutMapping("/{courseId}/teachers")
    public ApiResponse<List<CourseTeacherSummary>> assignTeachers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId,
            @Valid @RequestBody AssignTeachersRequest request) {
        return new ApiResponse<>(courseAdminService.assignTeachers(principal, courseId, request));
    }

    @DeleteMapping("/{courseId}/teachers/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTeacher(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId,
            @PathVariable @Min(1) Long teacherId) {
        courseAdminService.removeTeacher(principal, courseId, teacherId);
    }
}
