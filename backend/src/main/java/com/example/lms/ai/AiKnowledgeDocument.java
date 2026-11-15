package com.example.lms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Nguồn tri thức văn bản đã đăng ký vào pipeline ingestion (ERD 17.4). Quyền truy cập do Spring quyết định, không phải vector index. */
@Entity
@Table(name = "ai_knowledge_documents")
public class AiKnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "file_resource_id")
    private Long fileResourceId;

    @Column(name = "source_text", columnDefinition = "MEDIUMTEXT")
    private String sourceText;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private DocumentSourceType sourceType;

    @Column(name = "source_uri", length = 1000)
    private String sourceUri;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "subject_code", length = 100)
    private String subjectCode;

    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topic_tags_json")
    private String topicTagsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "index_status", nullable = false)
    private IndexStatus indexStatus;

    @Column(name = "index_version", nullable = false)
    private int indexVersion;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "created_by", nullable = false, updatable = false)
    private long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AiKnowledgeDocument() {
    }

    /** Tài liệu mới ở trạng thái PENDING, chờ worker ingestion. */
    public static AiKnowledgeDocument create(Long courseId, Long lessonId, String title, DocumentSourceType type,
            String sourceText, long createdBy, Instant now) {
        AiKnowledgeDocument d = new AiKnowledgeDocument();
        d.courseId = courseId;
        d.lessonId = lessonId;
        d.title = title;
        d.sourceType = type;
        d.sourceText = sourceText;
        d.createdBy = createdBy;
        d.indexStatus = IndexStatus.PENDING;
        d.indexVersion = 1;
        d.createdAt = now;
        d.updatedAt = now;
        return d;
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public Long getFileResourceId() {
        return fileResourceId;
    }

    public void setFileResourceId(Long fileResourceId) {
        this.fileResourceId = fileResourceId;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DocumentSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(DocumentSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getTopicTagsJson() {
        return topicTagsJson;
    }

    public void setTopicTagsJson(String topicTagsJson) {
        this.topicTagsJson = topicTagsJson;
    }

    public IndexStatus getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(IndexStatus indexStatus) {
        this.indexStatus = indexStatus;
    }

    public int getIndexVersion() {
        return indexVersion;
    }

    public void setIndexVersion(int indexVersion) {
        this.indexVersion = indexVersion;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(Long deletedBy) {
        this.deletedBy = deletedBy;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

}
