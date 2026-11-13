package com.example.lms.exam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.lms.contract.ai.LearningInteractionRecorder;

/**
 * Chuyển kết quả chấm sang phần AI của C (điểm giao 6) SAU KHI đã commit. Lỗi AI chỉ ghi log, không làm hỏng việc
 * chấm (BR-AI-11); C tự xử lý lại bằng job. Chưa có bean của C thì bỏ qua.
 */
@Component
public class ExamInteractionForwarder {

    private static final Logger log = LoggerFactory.getLogger(ExamInteractionForwarder.class);

    private final ObjectProvider<LearningInteractionRecorder> recorder;

    public ExamInteractionForwarder(ObjectProvider<LearningInteractionRecorder> recorder) {
        this.recorder = recorder;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExamGraded(ExamGradedEvent event) {
        recorder.ifAvailable(r -> {
            try {
                r.recordExamInteractions(event.attemptId());
            } catch (RuntimeException e) {
                log.warn("AI interaction recording failed for attempt {}; grading is unaffected",
                        event.attemptId(), e);
            }
        });
    }
}
