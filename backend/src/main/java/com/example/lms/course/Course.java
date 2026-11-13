package com.example.lms.course;

import com.example.lms.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "courses", uniqueConstraints = @UniqueConstraint(name = "uk_courses_slug", columnNames = "slug"))
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private CourseCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User createdBy;

    @Column(name = "thumbnail_file_id", columnDefinition = "BIGINT UNSIGNED")
    private Long thumbnailFileId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "original_price", nullable = false, columnDefinition = "BIGINT UNSIGNED DEFAULT 0")
    private long originalPrice;

    @Column(name = "sale_price", nullable = false, columnDefinition = "BIGINT UNSIGNED DEFAULT 0")
    private long salePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "subject_code", length = 100)
    private String subjectCode;

    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    @Column(name = "access_duration_days", columnDefinition = "INT UNSIGNED")
    private Integer accessDurationDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_rule", nullable = false, length = 60)
    private CourseCompletionRule completionRule = CourseCompletionRule.ALL_MANDATORY_LESSONS;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "hidden_at")
    private Instant hiddenAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Course() {
    }

    public Course(
            CourseCategory category,
            User createdBy,
            String title,
            String slug,
            String shortDescription,
            String description,
            long originalPrice,
            long salePrice,
            Long thumbnailFileId,
            String subjectCode,
            String gradeLevel,
            Integer accessDurationDays,
            CourseCompletionRule completionRule) {
        this.category = category;
        this.createdBy = createdBy;
        this.title = title;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.description = description;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.thumbnailFileId = thumbnailFileId;
        this.subjectCode = subjectCode;
        this.gradeLevel = gradeLevel;
        this.accessDurationDays = accessDurationDays;
        this.completionRule = completionRule == null
                ? CourseCompletionRule.ALL_MANDATORY_LESSONS
                : completionRule;
        this.status = CourseStatus.DRAFT;
    }

    @PrePersist
    void setCreatedAt() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void setUpdatedAt() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CourseCategory getCategory() {
        return category;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Long getThumbnailFileId() {
        return thumbnailFileId;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public long getOriginalPrice() {
        return originalPrice;
    }

    public long getSalePrice() {
        return salePrice;
    }

    public CourseStatus getStatus() {
        return status;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public Integer getAccessDurationDays() {
        return accessDurationDays;
    }

    public CourseCompletionRule getCompletionRule() {
        return completionRule;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            CourseCategory category,
            String title,
            String shortDescription,
            String description,
            long originalPrice,
            long salePrice,
            Long thumbnailFileId,
            String subjectCode,
            String gradeLevel,
            Integer accessDurationDays,
            CourseCompletionRule completionRule) {
        this.category = category;
        this.title = title;
        this.shortDescription = shortDescription;
        this.description = description;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.thumbnailFileId = thumbnailFileId;
        this.subjectCode = subjectCode;
        this.gradeLevel = gradeLevel;
        this.accessDurationDays = accessDurationDays;
        this.completionRule = completionRule;
    }
}
