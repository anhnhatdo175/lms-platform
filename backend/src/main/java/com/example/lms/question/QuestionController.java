package com.example.lms.question;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.contract.question.Difficulty;
import com.example.lms.question.dto.CreateQuestionRequest;
import com.example.lms.question.dto.QuestionTeacherView;
import com.example.lms.question.dto.UpdateQuestionRequest;
import com.example.lms.shared.pagination.PageResponse;
import com.example.lms.shared.web.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** OpenAPI {@code /teacher/courses/{courseId}/questions} và {@code /teacher/questions/*} (ngân hàng câu hỏi). */
@Validated
@RestController
@RequestMapping("/api/v1")
public class QuestionController {

    private final QuestionService service;
    private final CurrentUserProvider currentUser;

    public QuestionController(QuestionService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping("/teacher/courses/{courseId}/questions")
    public ResponseEntity<ApiResponse<QuestionTeacherView>> create(@PathVariable @Min(1) long courseId,
            @Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(service.create(currentUser.current(), courseId, request)));
    }

    @GetMapping("/teacher/courses/{courseId}/questions")
    public PageResponse<QuestionTeacherView> list(@PathVariable @Min(1) long courseId,
            @RequestParam(required = false) Difficulty difficulty, @RequestParam(required = false) String skillCode,
            @RequestParam(required = false) Boolean active, @RequestParam(required = false) String q,
            @RequestParam(required = false) QuestionSource source, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(currentUser.current(), courseId, difficulty, skillCode, active, q, source, page, size);
    }

    @GetMapping("/teacher/questions/{questionId}")
    public ApiResponse<QuestionTeacherView> get(@PathVariable @Min(1) long questionId) {
        return ApiResponse.of(service.get(currentUser.current(), questionId));
    }

    @PatchMapping("/teacher/questions/{questionId}")
    public ApiResponse<QuestionTeacherView> update(@PathVariable @Min(1) long questionId,
            @Valid @RequestBody UpdateQuestionRequest request) {
        return ApiResponse.of(service.update(currentUser.current(), questionId, request));
    }

    @DeleteMapping("/teacher/questions/{questionId}")
    public ResponseEntity<Void> deactivate(@PathVariable @Min(1) long questionId) {
        service.deactivate(currentUser.current(), questionId);
        return ResponseEntity.noContent().build();
    }
}
