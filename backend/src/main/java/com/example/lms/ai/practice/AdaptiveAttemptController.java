package com.example.lms.ai.practice;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.ai.practice.AttemptViews.AttemptHistory;
import com.example.lms.ai.practice.AttemptViews.AttemptResult;
import com.example.lms.ai.practice.AttemptViews.SubmitRequest;
import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.shared.web.ApiResponse;

/** OpenAPI tag {@code student}: nộp và xem lịch sử bài luyện tập AI. */
@RestController
public class AdaptiveAttemptController {

    private final AdaptiveAttemptService service;
    private final CurrentUserProvider currentUser;

    public AdaptiveAttemptController(AdaptiveAttemptService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    /** 201 khi chấm mới; nộp lại cùng clientEventId trả kết quả cũ với {@code duplicate=true} (200). */
    @PostMapping("/student/ai/adaptive-tasks/{taskId}/attempts")
    public ResponseEntity<ApiResponse<AttemptResult>> submit(@PathVariable long taskId,
            @RequestBody SubmitRequest request) {
        AttemptViews.Submitted s = service.submit(currentUser.current().id(), taskId, request);
        return ResponseEntity.status(s.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.of(s.result()));
    }

    @GetMapping("/student/ai/adaptive-tasks/{taskId}/attempts")
    public ApiResponse<List<AttemptHistory>> history(@PathVariable long taskId) {
        return ApiResponse.of(service.history(currentUser.current().id(), taskId));
    }
}
