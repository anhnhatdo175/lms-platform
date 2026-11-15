package com.example.lms.ai.ingestion;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.ai.AiKnowledgeDocument;
import com.example.lms.ai.AiKnowledgeDocumentRepository;
import com.example.lms.ai.SessionErrorCode;
import com.example.lms.ai.session.AiServiceClient;
import com.example.lms.ai.session.SessionStateMachine;

/**
 * Lập chỉ mục tài liệu: PENDING → PROCESSING → READY|FAILED, lease 5 phút, fencing theo {@code attempt_count}, tự retry
 * tối đa 3 lần với backoff (Spec 20A.14). Gọi AI service nằm NGOÀI transaction DB để không giữ kết nối trong lúc chờ.
 * Tài liệu đang xử lý, FAILED hoặc đã gỡ không có trong danh sách được phép truy xuất (chỉ READY, chưa xóa).
 * <p>Phạm vi: văn bản đã có trong {@code source_text}. Trích xuất text từ PDF/DOCX là bước tách riêng.
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);
    private static final int BATCH = 5;

    private final AiKnowledgeDocumentRepository documents;
    private final AiServiceClient client;
    private final Clock clock;

    public DocumentIngestionService(AiKnowledgeDocumentRepository documents, AiServiceClient client, Clock clock) {
        this.documents = documents;
        this.client = client;
        this.clock = clock;
    }

    @Transactional
    public List<Claimed> claim() {
        Instant now = clock.instant();
        return documents.findRunnable(now, PageRequest.of(0, BATCH)).stream()
                .filter(d -> d.getSourceText() != null && !d.getSourceText().isBlank()
                        && d.getCourseId() != null)
                .filter(d -> documents.claim(d.getId(), d.getAttemptCount(), now.plus(SessionStateMachine.LEASE),
                        now) == 1)
                .map(d -> new Claimed(d.getId(), d.getAttemptCount() + 1, d.getCourseId(), d.getLessonId(),
                        d.getTitle(), d.getSourceText()))
                .toList();
    }

    /** Gọi từ worker (qua proxy) sau {@link #claim()}; không mở transaction quanh lời gọi HTTP. */
    public void process(Claimed c) {
        try {
            client.ingest(new AiServiceClient.IngestCall(c.id(), c.courseId(), c.lessonId(), c.title(), c.text()));
        } catch (AiServiceClient.AiServiceException e) {
            handleFailure(c, e);
            return;
        } catch (RuntimeException e) {
            handleFailure(c, new AiServiceClient.AiServiceException(AiServiceClient.AiServiceException.Kind.UNAVAILABLE,
                    "unexpected ingestion error", e));
            return;
        }
        if (documents.markReady(c.id(), c.attempt(), clock.instant()) != 1) {
            log.warn("document {} attempt {} lost its lease; result discarded", c.id(), c.attempt());
        }
    }

    private void handleFailure(Claimed c, AiServiceClient.AiServiceException e) {
        Instant now = clock.instant();
        String message = SessionStateMachine.safeMessage(e.getMessage());
        // attempt_count đếm cả lần đầu: lần 1 là chạy gốc, retry tự động tối đa 3 lần nữa.
        int autoRetriesDone = c.attempt() - 1;
        boolean retry = e.kind() == AiServiceClient.AiServiceException.Kind.UNAVAILABLE
                && SessionStateMachine.canAutoRetry(SessionErrorCode.AI_UNAVAILABLE, autoRetriesDone);
        int updated = retry
                ? documents.markRequeued(c.id(), c.attempt(), now.plus(SessionStateMachine.backoff(autoRetriesDone)),
                        message, now)
                : documents.markFailed(c.id(), c.attempt(), message, now);
        if (updated != 1) {
            log.warn("document {} attempt {} lost its lease before recording failure", c.id(), c.attempt());
        }
    }

    /**
     * Gỡ tài liệu: đánh dấu xóa trước (retrieval loại ngay), rồi xóa vector. Lỗi xóa vector chỉ ghi log vì allow-list đã
     * chặn truy xuất; vector mồ côi được dọn khi gọi lại.
     */
    @Transactional
    public void remove(long documentId, long deletedBy) {
        AiKnowledgeDocument d = documents.findById(documentId).orElse(null);
        if (d == null || d.getDeletedAt() != null) {
            return;
        }
        Instant now = clock.instant();
        d.setDeletedAt(now);
        d.setDeletedBy(deletedBy);
        d.setUpdatedAt(now);
        documents.save(d);
        try {
            client.deleteDocument(documentId);
        } catch (RuntimeException e) {
            log.warn("could not delete vectors of document {}; access is already blocked", documentId, e);
        }
    }

    public record Claimed(long id, int attempt, long courseId, Long lessonId, String title, String text) {
    }
}
