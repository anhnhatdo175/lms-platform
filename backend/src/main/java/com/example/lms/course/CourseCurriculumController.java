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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping("/api/v1/teacher/courses/{courseId}/curriculum")
    public ApiResponse<TeacherCurriculumView> getCurriculum(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId) {
        return new ApiResponse<>(curriculumService.getCurriculum(principal, courseId));
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

    @PostMapping("/api/v1/teacher/lessons/{lessonId}/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LessonResourceSummary> attachLessonResource(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long lessonId,
            @Valid @RequestBody AttachResourceRequest request) {
        return new ApiResponse<>(curriculumService.attachLessonResource(principal, lessonId, request));
    }

    @DeleteMapping("/api/v1/teacher/lessons/{lessonId}/resources/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detachLessonResource(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long lessonId,
            @PathVariable @Min(1) Long resourceId) {
        curriculumService.detachLessonResource(principal, lessonId, resourceId);
    }

    @PutMapping("/api/v1/teacher/lessons/{lessonId}/prerequisite")
    public ApiResponse<LessonPrerequisiteRef> setLessonPrerequisite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long lessonId,
            @Valid @RequestBody PrerequisiteRequest request) {
        return new ApiResponse<>(curriculumService.setLessonPrerequisite(principal, lessonId, request));
    }

    @DeleteMapping("/api/v1/teacher/lessons/{lessonId}/prerequisite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLessonPrerequisite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long lessonId) {
        curriculumService.removeLessonPrerequisite(principal, lessonId);
    }

    @PutMapping("/api/v1/teacher/courses/{courseId}/curriculum/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderCurriculum(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId,
            @Valid @RequestBody CurriculumOrderRequest request) {
        curriculumService.reorderCurriculum(principal, courseId, request);
    }
}
