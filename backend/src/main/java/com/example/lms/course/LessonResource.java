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
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "lesson_resources")
public class LessonResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Lesson lesson;

    @Column(name = "file_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long resourceId;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private LessonResourceType resourceType;

    @Column(nullable = false)
    private boolean downloadable;

    @Column(nullable = false, columnDefinition = "INT UNSIGNED")
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LessonResource() {
    }

    public LessonResource(
            Lesson lesson,
            Long resourceId,
            String title,
            LessonResourceType resourceType,
            boolean downloadable,
            int position) {
        this.lesson = lesson;
        this.resourceId = resourceId;
        this.title = title;
        this.resourceType = resourceType;
        this.downloadable = downloadable;
        this.position = position;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getTitle() {
        return title;
    }

    public LessonResourceType getResourceType() {
        return resourceType;
    }

    public boolean isDownloadable() {
        return downloadable;
    }

    public int getPosition() {
        return position;
    }
}
