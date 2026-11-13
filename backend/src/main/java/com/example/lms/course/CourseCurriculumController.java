package com.example.lms.course;

import com.example.lms.auth.UserPrincipal;
import com.example.lms.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@Validated
@RestController
@PreAuthorize("hasRole('TEACHER')")
public class CourseCurriculumController {

    private final CourseCurriculumService curriculumService;

    public CourseCurriculumController(CourseCurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    @PostMapping("/api/v1/teacher/courses/{courseId}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SectionSummary> createSection(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId,
            @Valid @RequestBody CreateSectionRequest request) {
        return new ApiResponse<>(curriculumService.createSection(principal, courseId, request));
    }

    @PatchMapping("/api/v1/teacher/sections/{sectionId}")
    public ApiResponse<SectionSummary> updateSection(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long sectionId,
            @RequestBody JsonNode changes) {
        return new ApiResponse<>(curriculumService.updateSection(principal, sectionId, changes));
    }

    @DeleteMapping("/api/v1/teacher/sections/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long sectionId) {
        curriculumService.deleteSection(principal, sectionId);
    }

    @PostMapping("/api/v1/teacher/sections/{sectionId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LessonDetail> createLesson(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long sectionId,
            @Valid @RequestBody CreateLessonRequest request) {
        return new ApiResponse<>(curriculumService.createLesson(principal, sectionId, request));
    }

    @PatchMapping("/api/v1/teacher/lessons/{lessonId}")
    public ApiResponse<LessonDetail> updateLesson(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long lessonId,
            @RequestBody JsonNode changes) {
        return new ApiResponse<>(curriculumService.updateLesson(principal, lessonId, changes));
    }

    @DeleteMapping("/api/v1/teacher/lessons/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLesson(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long lessonId) {
        curriculumService.deleteLesson(principal, lessonId);
    }
}
