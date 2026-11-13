package com.example.lms.exam;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.exam.dto.AttemptRequests.StartExamAttemptRequest;
import com.example.lms.exam.dto.AttemptRequests.SubmitExamRequest;
import com.example.lms.exam.dto.ExamAnswerAck;
import com.example.lms.exam.dto.ExamAnswerRequest;
import com.example.lms.exam.dto.ExamAttemptResponse;
import com.example.lms.exam.dto.ExamAttemptSessionResponse;
import com.example.lms.exam.dto.ExamResultResponse;
import com.example.lms.exam.dto.ExamViewResponse;
import com.example.lms.shared.web.ApiResponse;

import jakarta.validation.Valid;

/** OpenAPI tag {@code exams}: phía Student. */
@RestController
public class ExamAttemptController {

    private final AttemptService attempts;
    private final CurrentUserProvider currentUser;

    public ExamAttemptController(AttemptService attempts, CurrentUserProvider currentUser) {
        this.attempts = attempts;
        this.currentUser = currentUser;
    }

    @GetMapping("/learning/exams/{examId}")
    public ApiResponse<ExamViewResponse> exam(@PathVariable long examId) {
        return ApiResponse.of(attempts.learningExam(currentUser.current().id(), examId));
    }

    /** 201 khi tạo Attempt mới, 200 khi trả lại Attempt IN_PROGRESS đang có. */
    @PostMapping("/exams/{examId}/attempts")
    public ResponseEntity<ApiResponse<ExamAttemptSessionResponse>> start(@PathVariable long examId,
            @RequestBody(required = false) StartExamAttemptRequest request) {
        AttemptService.StartResult result = attempts.start(currentUser.current().id(), examId);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.of(result.session()));
    }

    @GetMapping("/exam-attempts/{attemptId}")
    public ApiResponse<ExamAttemptSessionResponse> attempt(@PathVariable long attemptId) {
        return ApiResponse.of(attempts.attempt(currentUser.current().id(), attemptId));
    }

    @PutMapping("/exam-attempts/{attemptId}/answers/{attemptQuestionId}")
    public ApiResponse<ExamAnswerAck> saveAnswer(@PathVariable long attemptId, @PathVariable long attemptQuestionId,
            @Valid @RequestBody ExamAnswerRequest request) {
        return ApiResponse.of(attempts.saveAnswer(currentUser.current().id(), attemptId, attemptQuestionId, request));
    }

    @PostMapping("/exam-attempts/{attemptId}/submit")
    public ApiResponse<ExamAttemptResponse> submit(@PathVariable long attemptId,
            @RequestBody(required = false) SubmitExamRequest request) {
        return ApiResponse.of(attempts.submit(currentUser.current().id(), attemptId));
    }

    @GetMapping("/exam-attempts/{attemptId}/result")
    public ApiResponse<ExamResultResponse> result(@PathVariable long attemptId) {
        return ApiResponse.of(attempts.result(currentUser.current().id(), attemptId));
    }
}
