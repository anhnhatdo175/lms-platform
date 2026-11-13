package com.example.lms.exam;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.exam.dto.AddExamQuestionRequest;
import com.example.lms.exam.dto.CreateExamRequest;
import com.example.lms.exam.dto.ExamBuilderResponse;
import com.example.lms.exam.dto.ExamStatisticsResponse;
import com.example.lms.exam.dto.ExamSummaryResponse;
import com.example.lms.exam.dto.UpdateExamRequest;
import com.example.lms.shared.web.ApiResponse;

import jakarta.validation.Valid;

/** OpenAPI {@code /teacher/lessons/{lessonId}/exams} và {@code /teacher/exams*}. */
@RestController
public class TeacherExamController {

    private final ExamBuilderService builder;
    private final ExamStatisticsService statistics;
    private final CurrentUserProvider currentUser;

    public TeacherExamController(ExamBuilderService builder, ExamStatisticsService statistics,
            CurrentUserProvider currentUser) {
        this.builder = builder;
        this.statistics = statistics;
        this.currentUser = currentUser;
    }

    @PostMapping("/teacher/lessons/{lessonId}/exams")
    public ResponseEntity<ApiResponse<ExamSummaryResponse>> create(@PathVariable long lessonId,
            @Valid @RequestBody CreateExamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(builder.create(currentUser.current(), lessonId, request)));
    }

    @GetMapping("/teacher/exams/{examId}")
    public ApiResponse<ExamBuilderResponse> get(@PathVariable long examId) {
        return ApiResponse.of(builder.get(currentUser.current(), examId));
    }

    @PatchMapping("/teacher/exams/{examId}")
    public ApiResponse<ExamSummaryResponse> update(@PathVariable long examId,
            @Valid @RequestBody UpdateExamRequest request) {
        return ApiResponse.of(builder.update(currentUser.current(), examId, request));
    }

    @PostMapping("/teacher/exams/{examId}/questions")
    public ResponseEntity<ApiResponse<ExamBuilderResponse>> addQuestion(@PathVariable long examId,
            @Valid @RequestBody AddExamQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(builder.addQuestion(currentUser.current(), examId, request)));
    }

    @DeleteMapping("/teacher/exams/{examId}/questions/{questionId}")
    public ResponseEntity<Void> removeQuestion(@PathVariable long examId, @PathVariable long questionId) {
        builder.removeQuestion(currentUser.current(), examId, questionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/teacher/exams/{examId}/publish")
    public ApiResponse<ExamSummaryResponse> publish(@PathVariable long examId) {
        return ApiResponse.of(builder.publish(currentUser.current(), examId));
    }

    @PostMapping("/teacher/exams/{examId}/close")
    public ApiResponse<ExamSummaryResponse> close(@PathVariable long examId) {
        return ApiResponse.of(builder.close(currentUser.current(), examId));
    }

    @GetMapping("/teacher/exams/{examId}/statistics")
    public ApiResponse<ExamStatisticsResponse> statistics(@PathVariable long examId) {
        return ApiResponse.of(statistics.statistics(currentUser.current(), examId));
    }
}
