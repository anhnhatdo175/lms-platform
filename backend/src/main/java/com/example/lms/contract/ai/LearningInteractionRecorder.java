package com.example.lms.contract.ai;

/**
 * Điểm giao 6, C cung cấp. B gọi SAU KHI transaction chấm bài đã commit; lỗi ở đây không được làm hỏng việc
 * chấm (BR-AI-11). C phải idempotent theo {@code event_key = EXAMQ:{attempt_question_id}} và tự xử lý lại
 * các Attempt bỏ sót bằng job.
 */
public interface LearningInteractionRecorder {

    void recordExamInteractions(long attemptId);
}
