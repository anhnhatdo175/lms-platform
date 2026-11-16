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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class CourseWorkflowController {

    private final CourseWorkflowService workflowService;

    public CourseWorkflowController(CourseWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/teacher/courses/{courseId}/submit-review")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<ReviewRequestSummary> submitReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId,
            @Valid @RequestBody ReviewSubmissionRequest request) {
        return new ApiResponse<>(workflowService.submitReview(principal, courseId, request));
    }

    @PostMapping("/teacher/courses/{courseId}/withdraw-review")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<ReviewDecisionResult> withdrawReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId) {
        return new ApiResponse<>(workflowService.withdrawReview(principal, courseId));
    }

    @PostMapping("/teacher/courses/{courseId}/reopen")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<ReviewDecisionResult> reopen(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId) {
        return new ApiResponse<>(workflowService.reopen(principal, courseId));
    }

    @GetMapping("/admin/course-review-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ReviewRequestSummary> listReviewRequests(
            @RequestParam(required = false) CourseReviewStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort) {
        return workflowService.listReviewRequests(status, page, size, sort);
    }

    @PostMapping("/admin/courses/{courseId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReviewDecisionResult> approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId,
            @RequestBody(required = false) ReviewDecisionRequest request) {
        return new ApiResponse<>(workflowService.approve(principal, courseId, request));
    }

    @PostMapping("/admin/courses/{courseId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReviewDecisionResult> reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId,
            @Valid @RequestBody RejectCourseRequest request) {
        return new ApiResponse<>(workflowService.reject(principal, courseId, request));
    }

    @PostMapping("/admin/courses/{courseId}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReviewDecisionResult> hide(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId,
            @Valid @RequestBody HideCourseRequest request) {
        return new ApiResponse<>(workflowService.hide(principal, courseId, request));
    }

    @PostMapping("/admin/courses/{courseId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReviewDecisionResult> publishHidden(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId) {
        return new ApiResponse<>(workflowService.publishHidden(principal, courseId));
    }

    @PostMapping("/admin/courses/{courseId}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReviewDecisionResult> archive(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId,
            @Valid @RequestBody HideCourseRequest request) {
        return new ApiResponse<>(workflowService.archive(principal, courseId, request));
    }
}
