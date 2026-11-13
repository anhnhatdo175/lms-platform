package com.example.lms.exam.dto;

/**
 * Body của bắt đầu và nộp bài. Hai thao tác idempotent theo trạng thái (Attempt IN_PROGRESS có sẵn, Attempt đã
 * GRADED) nên không lưu {@code clientRequestId}/{@code clientEventId} (Doc phần B, Mục 13 ý 1, 2).
 */
public final class AttemptRequests {

    private AttemptRequests() {
    }

    public record StartExamAttemptRequest(String clientRequestId) {
    }

    public record SubmitExamRequest(String clientEventId) {
    }
}
