package com.example.lms.course;

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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "lessons",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lessons_position",
                columnNames = {"section_id", "position"}))
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private CourseSection section;

    @Column(name = "video_file_id", columnDefinition = "BIGINT UNSIGNED")
    private Long videoResourceId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 20)
    private LessonType lessonType;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "duration_seconds", columnDefinition = "INT UNSIGNED")
    private Integer durationSeconds;

    @Column(nullable = false, columnDefinition = "INT UNSIGNED")
    private int position;

    @Column(nullable = false)
    private boolean preview;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private boolean mandatory;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_method", nullable = false, length = 30)
    private CompletionMethod completionMethod;

    @Column(name = "completion_threshold", precision = 5, scale = 2)
    private BigDecimal completionThreshold;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Lesson() {
    }

    public Lesson(
            CourseSection section,
            String title,
            String description,
            String content,
            LessonType lessonType,
            Long videoResourceId,
            Integer durationSeconds,
            int position,
            boolean preview,
            boolean published,
            boolean mandatory,
            CompletionMethod completionMethod,
            BigDecimal completionThreshold) {
        this.section = section;
        this.title = title;
        this.description = description;
        this.content = content;
        this.lessonType = lessonType;
        this.videoResourceId = videoResourceId;
        this.durationSeconds = durationSeconds;
        this.position = position;
        this.preview = preview;
        this.published = published;
        this.mandatory = mandatory;
        this.completionMethod = completionMethod;
        this.completionThreshold = completionThreshold;
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

    public CourseSection getSection() {
        return section;
    }

    public Long getVideoResourceId() {
        return videoResourceId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LessonType getLessonType() {
        return lessonType;
    }

    public String getContent() {
        return content;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public int getPosition() {
        return position;
    }

    public boolean isPreview() {
        return preview;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public CompletionMethod getCompletionMethod() {
        return completionMethod;
    }

    public BigDecimal getCompletionThreshold() {
        return completionThreshold;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            String title,
            boolean updateDescription,
            String description,
            boolean updateContent,
            String content,
            boolean updateVideoResource,
            Long videoResourceId,
            boolean updateDuration,
            Integer durationSeconds,
            Boolean preview,
            Boolean published,
            Boolean mandatory,
            CompletionMethod completionMethod,
            boolean updateCompletionThreshold,
            BigDecimal completionThreshold) {
        if (title != null) this.title = title;
        if (updateDescription) this.description = description;
        if (updateContent) this.content = content;
        if (updateVideoResource) this.videoResourceId = videoResourceId;
        if (updateDuration) this.durationSeconds = durationSeconds;
        if (preview != null) this.preview = preview;
        if (published != null) this.published = published;
        if (mandatory != null) this.mandatory = mandatory;
        if (completionMethod != null) this.completionMethod = completionMethod;
        if (updateCompletionThreshold) this.completionThreshold = completionThreshold;
    }
}
