package com.example.lms.ai.ingestion;

import java.time.Instant;
import java.util.List;

import com.example.lms.ai.DocumentSourceType;
import com.example.lms.ai.IndexStatus;

public final class KnowledgeDocumentViews {

    private KnowledgeDocumentViews() {
    }

    /** Hợp nhất hai biến thể oneOf của OpenAPI; {@link KnowledgeDocumentService} kiểm tra loại trừ lẫn nhau (422). */
    public record CreateRequest(DocumentSourceType sourceType, String title, Long courseId, Long lessonId,
            String subjectCode, String gradeLevel, List<String> topicTags, String languageCode, Long fileResourceId,
            String textContent) {
    }

    public record DocumentView(long id, String title, DocumentSourceType sourceType, long courseId, Long lessonId,
            String subjectCode, String gradeLevel, List<String> topicTags, String languageCode, IndexStatus status,
            Integer chunkCount, String errorCode, String errorMessage, int indexVersion, Instant createdAt,
            Instant updatedAt, Instant deletedAt) {
    }
}
