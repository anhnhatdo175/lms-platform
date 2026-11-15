package com.example.lms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Mức thành thạo M_k theo kỹ năng, cập nhật bằng EMA (ERD 17.7, BR-AI-14). */
@Entity
@Table(name = "learner_skill_states")
public class LearnerSkillState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private long studentId;

    @Column(name = "skill_code", nullable = false, updatable = false, length = 150)
    private String skillCode;

    @Column(name = "skill_name", nullable = false, length = 255)
    private String skillName;

    @Column(name = "subject_code", length = 100)
    private String subjectCode;

    @Column(name = "mastery_score", nullable = false, precision = 6, scale = 5)
    private BigDecimal masteryScore;

    @Column(name = "evidence_count", nullable = false)
    private int evidenceCount;

    @Column(name = "last_evidence_at")
    private Instant lastEvidenceAt;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LearnerSkillState() {
    }

    public Long getId() {
        return id;
    }

    public long getStudentId() {
        return studentId;
    }

    public String getSkillCode() {
        return skillCode;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public BigDecimal getMasteryScore() {
        return masteryScore;
    }

    public void setMasteryScore(BigDecimal masteryScore) {
        this.masteryScore = masteryScore;
    }

    public int getEvidenceCount() {
        return evidenceCount;
    }

    public void setEvidenceCount(int evidenceCount) {
        this.evidenceCount = evidenceCount;
    }

    public Instant getLastEvidenceAt() {
        return lastEvidenceAt;
    }

    public void setLastEvidenceAt(Instant lastEvidenceAt) {
        this.lastEvidenceAt = lastEvidenceAt;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

}
