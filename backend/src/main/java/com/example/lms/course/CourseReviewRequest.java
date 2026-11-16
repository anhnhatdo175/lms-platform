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
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "course_review_requests")
public class CourseReviewRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", columnDefinition = "BIGINT UNSIGNED")
    private User reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseReviewStatus status;

    @Column(name = "submission_note", columnDefinition = "TEXT")
    private String submissionNote;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CourseReviewRequest() {
    }

    public CourseReviewRequest(Course course, User submittedBy, String submissionNote) {
        this.course = course;
        this.submittedBy = submittedBy;
        this.submissionNote = submissionNote;
        this.status = CourseReviewStatus.PENDING;
    }

    @PrePersist
    void setCreatedAt() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (submittedAt == null) submittedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public User getSubmittedBy() {
        return submittedBy;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public CourseReviewStatus getStatus() {
        return status;
    }

    public String getSubmissionNote() {
        return submissionNote;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void complete(CourseReviewStatus status, User reviewer, String reviewNote, Instant reviewedAt) {
        this.status = status;
        this.reviewedBy = reviewer;
        this.reviewNote = reviewNote;
        this.reviewedAt = reviewedAt;
    }
}
