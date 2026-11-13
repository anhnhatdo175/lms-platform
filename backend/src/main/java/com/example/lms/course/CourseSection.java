package com.example.lms.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.Instant;

@Entity
@Table(
        name = "course_sections",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_sections_position",
                columnNames = {"course_id", "position"}))
public class CourseSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Course course;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "INT UNSIGNED")
    private int position;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "release_after_days", nullable = false, columnDefinition = "INT UNSIGNED DEFAULT 0")
    private int releaseAfterDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CourseSection() {
    }

    public CourseSection(
            Course course,
            String title,
            String description,
            int position,
            boolean published,
            int releaseAfterDays) {
        this.course = course;
        this.title = title;
        this.description = description;
        this.position = position;
        this.published = published;
        this.releaseAfterDays = releaseAfterDays;
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

    public Course getCourse() {
        return course;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPosition() {
        return position;
    }

    public boolean isPublished() {
        return published;
    }

    public int getReleaseAfterDays() {
        return releaseAfterDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String title, String description, boolean updateDescription, Boolean published, Integer releaseAfterDays) {
        if (title != null) this.title = title;
        if (updateDescription) this.description = description;
        if (published != null) this.published = published;
        if (releaseAfterDays != null) this.releaseAfterDays = releaseAfterDays;
    }
}
