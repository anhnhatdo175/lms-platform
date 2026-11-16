package com.example.lms.learning;

import com.example.lms.auth.UserPrincipal;
import com.example.lms.common.ApiResponse;
import com.example.lms.course.CourseProgressService.CompleteResult;
import com.example.lms.course.CourseProgressService.HeartbeatResult;
import com.example.lms.course.CourseProgressService.ProgressView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/learning")
@PreAuthorize("hasRole('STUDENT')")
public class LearningController {

    private final LearningService learning;

    public LearningController(LearningService learning) {
        this.learning = learning;
    }

    @GetMapping("/lessons/{lessonId}")
    public ApiResponse<LessonLearningView> getLesson(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long lessonId) {
        return new ApiResponse<>(learning.getLesson(principal.id(), lessonId));
    }

    @PostMapping("/lessons/{lessonId}/heartbeat")
    public ApiResponse<HeartbeatResult> heartbeat(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long lessonId,
            @Valid @RequestBody LessonHeartbeatRequest request) {
        return new ApiResponse<>(learning.heartbeat(principal.id(), lessonId, request));
    }

    @PostMapping("/lessons/{lessonId}/complete")
    public ApiResponse<CompleteResult> complete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long lessonId,
            @Valid @RequestBody CompleteLessonRequest request) {
        return new ApiResponse<>(learning.complete(principal.id(), lessonId, request));
    }

    @GetMapping("/courses/{courseId}/progress")
    public ApiResponse<ProgressView> getCourseProgress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Min(1) Long courseId) {
        return new ApiResponse<>(learning.courseProgress(principal.id(), courseId));
    }
}
