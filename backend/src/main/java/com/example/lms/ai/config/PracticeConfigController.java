package com.example.lms.ai.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.ai.config.PracticeConfigViews.Request;
import com.example.lms.ai.config.PracticeConfigViews.View;
import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.shared.web.ApiResponse;

/** OpenAPI tag {@code teacher}: cấu hình luyện tập AI của Course và ghi đè theo Lesson. */
@RestController
public class PracticeConfigController {

    private final PracticeConfigService service;
    private final CurrentUserProvider currentUser;

    public PracticeConfigController(PracticeConfigService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/teacher/courses/{courseId}/ai-config")
    public ApiResponse<View> getCourse(@PathVariable long courseId) {
        return ApiResponse.of(service.getForCourse(currentUser.current(), courseId));
    }

    @PutMapping("/teacher/courses/{courseId}/ai-config")
    public ApiResponse<View> putCourse(@PathVariable long courseId, @RequestBody Request request) {
        return ApiResponse.of(service.upsertForCourse(currentUser.current(), courseId, request));
    }

    @GetMapping("/teacher/lessons/{lessonId}/ai-config")
    public ApiResponse<View> getLesson(@PathVariable long lessonId) {
        return ApiResponse.of(service.getForLesson(currentUser.current(), lessonId));
    }

    @PutMapping("/teacher/lessons/{lessonId}/ai-config")
    public ApiResponse<View> putLesson(@PathVariable long lessonId, @RequestBody Request request) {
        return ApiResponse.of(service.upsertForLesson(currentUser.current(), lessonId, request));
    }

    @DeleteMapping("/teacher/lessons/{lessonId}/ai-config")
    public ResponseEntity<Void> deleteLesson(@PathVariable long lessonId) {
        service.deleteForLesson(currentUser.current(), lessonId);
        return ResponseEntity.noContent().build();
    }
}
