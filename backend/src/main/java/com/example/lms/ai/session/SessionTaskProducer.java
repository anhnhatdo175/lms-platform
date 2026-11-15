package com.example.lms.ai.session;

import java.util.List;

/** Tạo danh sách nhiệm vụ cho một phiên; ném {@link SessionFailure} khi không thể tạo nhiệm vụ đáng tin cậy. */
public interface SessionTaskProducer {

    Result produce(SessionSnapshot session);

    /**
     * Như {@link #produce(SessionSnapshot)}, nhưng gọi {@code heartbeat} sau mỗi bước dài (mỗi lần gọi AI) để worker gia
     * hạn lease. {@code heartbeat} ném {@link SessionProcessor.LeaseLost} khi lease đã mất, để dừng sớm.
     */
    default Result produce(SessionSnapshot session, Runnable heartbeat) {
        return produce(session);
    }

    record Result(List<TaskDraft> tasks, String pipelineVersion, String retrievalTraceJson) {
        public Result {
            tasks = List.copyOf(tasks);
        }
    }
}
