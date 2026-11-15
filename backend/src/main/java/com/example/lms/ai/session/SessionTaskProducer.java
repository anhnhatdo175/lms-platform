package com.example.lms.ai.session;

import java.util.List;

/** Tạo danh sách nhiệm vụ cho một phiên; ném {@link SessionFailure} khi không thể tạo nhiệm vụ đáng tin cậy. */
public interface SessionTaskProducer {

    Result produce(SessionSnapshot session);

    record Result(List<TaskDraft> tasks, String pipelineVersion, String retrievalTraceJson) {
        public Result {
            tasks = List.copyOf(tasks);
        }
    }
}
