package com.example.lms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Hồ sơ tổng hợp P(s) của người học (ERD 17.6); mức thành thạo theo kỹ năng nằm ở LearnerSkillState. */
@Entity
@Table(name = "learner_profiles")
public class LearnerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private long studentId;

    @Column(name = "average_score", precision = 6, scale = 5)
    private BigDecimal averageScore;

    @Column(name = "score_stddev", precision = 6, scale = 5)
    private BigDecimal scoreStddev;

    @Column(name = "learning_pace", precision = 10, scale = 4)
    private BigDecimal learningPace;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strength_topics_json")
    private String strengthTopicsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weak_topics_json")
    private String weakTopicsJson;

    @Column(name = "profile_vector_id", length = 255)
    private String profileVectorId;

    @Column(name = "profile_vector_version", nullable = false)
    private int profileVectorVersion;

    @Column(name = "profile_version", nullable = false)
    private int profileVersion;

    @Column(name = "last_interaction_at")
    private Instant lastInteractionAt;

    @Column(name = "computed_at")
    private Instant computedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LearnerProfile() {
    }

    public static LearnerProfile create(long studentId, Instant now) {
        LearnerProfile p = new LearnerProfile();
        p.studentId = studentId;
        p.profileVersion = 1;
        p.profileVectorVersion = 1;
        p.createdAt = now;
        p.updatedAt = now;
        return p;
    }

    public Long getId() {
        return id;
    }

    public long getStudentId() {
        return studentId;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    public BigDecimal getScoreStddev() {
        return scoreStddev;
    }

    public void setScoreStddev(BigDecimal scoreStddev) {
        this.scoreStddev = scoreStddev;
    }

    public BigDecimal getLearningPace() {
        return learningPace;
    }

    public void setLearningPace(BigDecimal learningPace) {
        this.learningPace = learningPace;
    }

    public String getStrengthTopicsJson() {
        return strengthTopicsJson;
    }

    public void setStrengthTopicsJson(String strengthTopicsJson) {
        this.strengthTopicsJson = strengthTopicsJson;
    }

    public String getWeakTopicsJson() {
        return weakTopicsJson;
    }

    public void setWeakTopicsJson(String weakTopicsJson) {
        this.weakTopicsJson = weakTopicsJson;
    }

    public String getProfileVectorId() {
        return profileVectorId;
    }

    public void setProfileVectorId(String profileVectorId) {
        this.profileVectorId = profileVectorId;
    }

    public int getProfileVectorVersion() {
        return profileVectorVersion;
    }

    public void setProfileVectorVersion(int profileVectorVersion) {
        this.profileVectorVersion = profileVectorVersion;
    }

    public int getProfileVersion() {
        return profileVersion;
    }

    public void setProfileVersion(int profileVersion) {
        this.profileVersion = profileVersion;
    }

    public Instant getLastInteractionAt() {
        return lastInteractionAt;
    }

    public void setLastInteractionAt(Instant lastInteractionAt) {
        this.lastInteractionAt = lastInteractionAt;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
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
