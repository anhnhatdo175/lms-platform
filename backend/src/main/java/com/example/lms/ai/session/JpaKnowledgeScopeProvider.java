package com.example.lms.ai.session;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.ai.AiKnowledgeDocumentRepository;

@Component
public class JpaKnowledgeScopeProvider implements KnowledgeScopeProvider {

    private final AiKnowledgeDocumentRepository documents;

    public JpaKnowledgeScopeProvider(AiKnowledgeDocumentRepository documents) {
        this.documents = documents;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> readyDocumentIds(long courseId, Long lessonId) {
        return lessonId == null ? documents.findReadyIdsByCourse(courseId)
                : documents.findReadyIdsByCourseAndLesson(courseId, lessonId);
    }
}
