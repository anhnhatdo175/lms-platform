package com.example.lms.ai.session;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.ai.session.AdaptiveSessionViews.CreateRequest;
import com.example.lms.ai.session.AdaptiveSessionViews.PracticeOptions;
import com.example.lms.ai.session.AdaptiveSessionViews.SessionView;
import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.shared.pagination.PageRequests;
import com.example.lms.shared.pagination.PageResponse;
import com.example.lms.shared.web.ApiResponse;

/** OpenAPI tag {@code student}: phiên nhiệm vụ thích ứng. */
@RestController
public class AdaptiveSessionController {

    private final AdaptiveSessionService service;
    private final CurrentUserProvider currentUser;

    public AdaptiveSessionController(AdaptiveSessionService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/student/ai/practice-options")
    public ApiResponse<PracticeOptions> options(@RequestParam long courseId,
            @RequestParam(required = false) Long lessonId) {
        return ApiResponse.of(service.practiceOptions(currentUser.current().id(), courseId, lessonId));
    }

    /** Luôn 202: phiên mới hoặc phiên cũ trả lại theo clientRequestId. */
    @PostMapping("/student/ai/adaptive-task-sessions")
    public ResponseEntity<ApiResponse<SessionView>> create(@RequestBody CreateRequest request) {
        AdaptiveSessionService.Created created = service.create(currentUser.current().id(), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.of(created.session()));
    }

    @GetMapping("/student/ai/adaptive-task-sessions")
    public PageResponse<SessionView> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(service.list(currentUser.current().id(),
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"))), v -> v);
    }

    @GetMapping("/student/ai/adaptive-task-sessions/{sessionId}")
    public ApiResponse<SessionView> get(@PathVariable long sessionId) {
        return ApiResponse.of(service.get(currentUser.current().id(), sessionId));
    }

    @PostMapping("/student/ai/adaptive-task-sessions/{sessionId}/retry")
    public ResponseEntity<ApiResponse<SessionView>> retry(@PathVariable long sessionId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(service.retry(currentUser.current().id(), sessionId)));
    }
}
