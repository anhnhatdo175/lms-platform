package com.example.lms.ai.session;

import java.util.List;
import java.util.Map;

/**
 * Hợp đồng nội bộ Spring -> AI service Python ({@code ai-service/lmsrag/api.py}). Spring đã quyết định quyền: client chỉ
 * gửi danh sách khóa học/tài liệu được phép, AI service không tự kiểm tra quyền (BR-AI-01).
 */
public interface AiServiceClient {

    /** @throws AiServiceException khi dịch vụ AI không khả dụng hoặc trả đầu ra không hợp lệ */
    GenerateOutcome generate(GenerateCall call);

    /** Lập chỉ mục văn bản của một tài liệu; trả số chunk đã index. */
    default int ingest(IngestCall call) {
        throw new AiServiceException(AiServiceException.Kind.UNAVAILABLE, "ingest is not supported by this client");
    }

    /** Trích văn bản từ tệp (txt, md, pdf, docx). INVALID_OUTPUT nếu tệp không đọc được hoặc không có chữ. */
    default String extractText(String filename, byte[] content) {
        throw new AiServiceException(AiServiceException.Kind.UNAVAILABLE, "extract is not supported by this client");
    }

    /** Xóa mọi vector của tài liệu (idempotent). */
    default void deleteDocument(long documentId) {
        throw new AiServiceException(AiServiceException.Kind.UNAVAILABLE, "delete is not supported by this client");
    }

    record IngestCall(long documentId, long courseId, Long lessonId, String title, String text) {
    }

    record GenerateCall(String query, List<Long> allowedCourseIds, List<Long> allowedDocumentIds, String targetSkill,
            double targetDifficulty, String language, Map<String, Object> learnerProfile) {
    }

    /**
     * @param sufficient {@code false} khi AI service trả {@code insufficient_context} (không đủ bằng chứng)
     * @param answerKey  chỉ lưu phía máy chủ, không bao giờ trả cho Student trước khi nộp (AC-AI-12)
     */
    record GenerateOutcome(boolean sufficient, String prompt, Object answerKey, String responseType,
            List<String> hints, String personalizationReason, List<String> supportingDocIds,
            List<String> retrievedDocIds, String llmBackend) {

        public static GenerateOutcome insufficient(List<String> retrievedDocIds, String llmBackend) {
            return new GenerateOutcome(false, null, null, null, List.of(), null, List.of(), retrievedDocIds,
                    llmBackend);
        }
    }

    class AiServiceException extends RuntimeException {

        /** UNAVAILABLE -> AI_UNAVAILABLE; INVALID_OUTPUT -> LLM_INVALID_OUTPUT. */
        public enum Kind {
            UNAVAILABLE, INVALID_OUTPUT
        }

        private final Kind kind;

        public AiServiceException(Kind kind, String message) {
            super(message);
            this.kind = kind;
        }

        public AiServiceException(Kind kind, String message, Throwable cause) {
            super(message, cause);
            this.kind = kind;
        }

        public Kind kind() {
            return kind;
        }
    }
}
