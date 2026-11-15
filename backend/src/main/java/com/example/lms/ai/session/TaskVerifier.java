package com.example.lms.ai.session;

import java.util.List;

/**
 * Bước kiểm tra nhiệm vụ GENERATED trước khi hiển thị (BR-AI-07, BR-AI-17); chạy ở phía Spring, tách khỏi bước sinh.
 * Trả danh sách lỗi; rỗng nghĩa là hợp lệ. Nhiệm vụ không qua thì không được hiển thị cho Student.
 */
public interface TaskVerifier {

    List<String> verify(AiServiceClient.GenerateOutcome outcome);
}
