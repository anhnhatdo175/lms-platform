package com.example.lms.ai.ingestion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.ai.AiKnowledgeDocument;
import com.example.lms.ai.AiKnowledgeDocumentRepository;
import com.example.lms.ai.DocumentSourceType;
import com.example.lms.ai.IndexStatus;
import com.example.lms.ai.ingestion.KnowledgeDocumentViews.CreateRequest;
import com.example.lms.ai.ingestion.KnowledgeDocumentViews.DocumentView;
import com.example.lms.ai.session.JsonSupport;
import com.example.lms.contract.audit.AuditLogService;
import com.example.lms.contract.course.CourseQueryService;
import com.example.lms.contract.identity.AccessPolicy;
import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.file.FileResource;
import com.example.lms.file.FileResourceRepository;
import com.example.lms.file.FileStatus;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Tài liệu AI phía Teacher/Admin (OpenAPI {@code teacher}). Quyền theo Course (Teacher chỉ thấy Course mình quản lý,
 * Admin thấy tất cả); tài liệu ngoài phạm vi trả 404. Tạo và index lại chỉ đặt PENDING: việc nặng do
 * {@link DocumentIngestionWorker} làm bất đồng bộ.
 */
@Service
public class KnowledgeDocumentService {

    static final Set<String> FILE_TYPES = Set.of(".pdf", ".docx", ".txt", ".md");
    static final int MAX_TEXT = 200_000;
    private static final Set<String> FILE_PURPOSES = Set.of("AI_SOURCE", "LESSON_RESOURCE");

    private final AiKnowledgeDocumentRepository documents;
    private final FileResourceRepository files;
    private final AccessPolicy access;
    private final CourseQueryService courses;
    private final DocumentIngestionService ingestion;
    private final AuditLogService audit;
    private final Clock clock;

    public KnowledgeDocumentService(AiKnowledgeDocumentRepository documents, FileResourceRepository files,
            AccessPolicy access, CourseQueryService courses, DocumentIngestionService ingestion,
            AuditLogService audit, Clock clock) {
        this.documents = documents;
        this.files = files;
        this.access = access;
        this.courses = courses;
        this.ingestion = ingestion;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public DocumentView create(CurrentUser user, CreateRequest r) {
        validate(r);
        requireManage(user, r.courseId());
        if (r.lessonId() != null && courses.findLesson(r.lessonId()).filter(l -> l.courseId() == r.courseId())
                .isEmpty()) {
            throw BusinessException.notFound("Lesson");
        }
        String text = null;
        Long fileId = null;
        String hash;
        if (r.sourceType() == DocumentSourceType.PASTED_TEXT) {
            text = r.textContent();
            hash = sha256(text);
        } else {
            FileResource file = requireUsableFile(user, r.fileResourceId());
            fileId = file.getId();
            hash = file.getChecksum();
        }
        if (hash != null && documents.existsByCourseIdAndContentHashAndDeletedAtIsNull(r.courseId(), hash)) {
            throw new BusinessException(ErrorCode.CONFLICT, "This content is already registered for the course");
        }
        AiKnowledgeDocument d = AiKnowledgeDocument.create(r.courseId(), r.lessonId(), r.title().strip(),
                r.sourceType(), text, user.id(), clock.instant());
        d.setFileResourceId(fileId);
        d.setContentHash(hash);
        d.setSubjectCode(blankToNull(r.subjectCode()));
        d.setGradeLevel(blankToNull(r.gradeLevel()));
        d.setLanguageCode(blankToNull(r.languageCode()) == null ? "vi" : r.languageCode().strip());
        d.setTopicTagsJson(r.topicTags() == null || r.topicTags().isEmpty() ? null : JsonSupport.toJson(r.topicTags()));
        documents.saveAndFlush(d);
        audit.record(new AuditLogService.AuditEntry(user.id(), "AI_DOCUMENT_CREATE", "AI_KNOWLEDGE_DOCUMENT",
                d.getId(), null, JsonSupport.toJson(java.util.Map.of("courseId", r.courseId(), "sourceType",
                        r.sourceType())), null));
        return view(d);
    }

    @Transactional(readOnly = true)
    public Page<DocumentView> list(CurrentUser user, Long courseId, Long lessonId, String status, Pageable pageable) {
        if (courseId == null && !user.isAdmin()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "courseId is required");
        }
        if (courseId != null) {
            requireManage(user, courseId);
        }
        IndexStatus parsed = null;
        if (status != null && !status.isBlank()) {
            try {
                parsed = IndexStatus.valueOf(status.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unknown status");
            }
        }
        return documents.search(courseId, lessonId, parsed, pageable).map(KnowledgeDocumentService::view);
    }

    @Transactional(readOnly = true)
    public DocumentView get(CurrentUser user, long id) {
        return view(managed(user, id));
    }

    @Transactional
    public void delete(CurrentUser user, long id) {
        AiKnowledgeDocument d = managed(user, id);
        ingestion.remove(d.getId(), user.id());
        audit.record(new AuditLogService.AuditEntry(user.id(), "AI_DOCUMENT_DELETE", "AI_KNOWLEDGE_DOCUMENT",
                d.getId(), null, null, null));
    }

    @Transactional
    public DocumentView reindex(CurrentUser user, long id) {
        AiKnowledgeDocument d = managed(user, id);
        if (documents.markReindex(d.getId(), clock.instant()) != 1) {
            throw BusinessException.stateConflict("Document is being processed; try again later");
        }
        audit.record(new AuditLogService.AuditEntry(user.id(), "AI_DOCUMENT_REINDEX", "AI_KNOWLEDGE_DOCUMENT",
                d.getId(), null, null, null));
        return view(documents.findById(id).orElseThrow(() -> BusinessException.notFound("Document")));
    }

    // ---- nội bộ ----

    private AiKnowledgeDocument managed(CurrentUser user, long id) {
        AiKnowledgeDocument d = documents.findById(id).filter(x -> x.getDeletedAt() == null)
                .orElseThrow(() -> BusinessException.notFound("Document"));
        // Không lộ sự tồn tại tài liệu của Course khác (Spec 7.4).
        if (d.getCourseId() == null || !canManage(user, d.getCourseId())) {
            throw BusinessException.notFound("Document");
        }
        return d;
    }

    private boolean canManage(CurrentUser user, long courseId) {
        return user.isAdmin() || access.canManageCourse(user.id(), courseId);
    }

    private void requireManage(CurrentUser user, long courseId) {
        if (!canManage(user, courseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You do not manage this course");
        }
    }

    private FileResource requireUsableFile(CurrentUser user, Long fileResourceId) {
        FileResource file = files.findById(fileResourceId).orElseThrow(() -> BusinessException.notFound("File"));
        if (!user.isAdmin() && !file.getUploadedBy().getId().equals(user.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You can only use files you uploaded");
        }
        if (file.getStatus() != FileStatus.READY) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, 422, "File is not ready");
        }
        String purpose = file.getPurpose().name();
        String name = file.getOriginalName() == null ? "" : file.getOriginalName().toLowerCase(Locale.ROOT);
        if (!FILE_PURPOSES.contains(purpose) || FILE_TYPES.stream().noneMatch(name::endsWith)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, 422,
                    "File must be a pdf, docx, txt or md document");
        }
        return file;
    }

    /** oneOf: LMS_FILE ⇔ fileResourceId, PASTED_TEXT ⇔ textContent; sai/thiếu/thừa → 422. */
    static void validate(CreateRequest r) {
        if (r == null || r.sourceType() == null || r.courseId() == null || r.courseId() < 1
                || r.title() == null || r.title().isBlank() || r.title().length() > 255
                || (r.lessonId() != null && r.lessonId() < 1)) {
            throw invalid("sourceType, title and courseId are required");
        }
        switch (r.sourceType()) {
            case LMS_FILE -> {
                if (r.fileResourceId() == null || r.fileResourceId() < 1 || r.textContent() != null) {
                    throw invalid("LMS_FILE requires fileResourceId and forbids textContent");
                }
            }
            case PASTED_TEXT -> {
                if (r.textContent() == null || r.textContent().isBlank() || r.textContent().length() > MAX_TEXT
                        || r.fileResourceId() != null) {
                    throw invalid("PASTED_TEXT requires textContent (1-200000 chars) and forbids fileResourceId");
                }
            }
            default -> throw invalid("sourceType must be LMS_FILE or PASTED_TEXT");
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, 422, message);
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    static DocumentView view(AiKnowledgeDocument d) {
        List<String> tags = JsonSupport.stringList(d.getTopicTagsJson());
        return new DocumentView(d.getId(), d.getTitle(), d.getSourceType(), d.getCourseId(), d.getLessonId(),
                d.getSubjectCode(), d.getGradeLevel(), tags, d.getLanguageCode(), d.getIndexStatus(), null, null,
                d.getErrorMessage(), d.getIndexVersion(), d.getCreatedAt(), d.getUpdatedAt(), d.getDeletedAt());
    }
}
