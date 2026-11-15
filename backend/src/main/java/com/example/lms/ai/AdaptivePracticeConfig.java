package com.example.lms.ai;

import com.example.lms.contract.question.Difficulty;
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

/** Cấu hình ràng buộc sư phạm C do Teacher đặt (ERD 17.3). Lesson ghi đè Course. */
@Entity
@Table(name = "adaptive_practice_configs")
public class AdaptivePracticeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private long courseId;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "scope_key", insertable = false, updatable = false)
    private Long scopeKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "subject_code", length = 100)
    private String subjectCode;

    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_skills_json", nullable = false)
    private String allowedSkillsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_difficulty", nullable = false)
    private Difficulty minDifficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "max_difficulty", nullable = false)
    private Difficulty maxDifficulty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_task_types_json", nullable = false)
    private String allowedTaskTypesJson;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "max_tasks_per_session", nullable = false)
    private int maxTasksPerSession;

    @Column(name = "socratic_enabled", nullable = false)
    private boolean socraticEnabled;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "max_hints_per_task", nullable = false)
    private int maxHintsPerTask;

    @Column(name = "updated_by", nullable = false)
    private long updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AdaptivePracticeConfig() {
    }

    public Long getId() {
        return id;
    }

    public long getCourseId() {
        return courseId;
    }

    public void setCourseId(long courseId) {
        this.courseId = courseId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public Long getScopeKey() {
        return scopeKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public String getAllowedSkillsJson() {
        return allowedSkillsJson;
    }

    public void setAllowedSkillsJson(String allowedSkillsJson) {
        this.allowedSkillsJson = allowedSkillsJson;
    }

    public Difficulty getMinDifficulty() {
        return minDifficulty;
    }

    public void setMinDifficulty(Difficulty minDifficulty) {
        this.minDifficulty = minDifficulty;
    }

    public Difficulty getMaxDifficulty() {
        return maxDifficulty;
    }

    public void setMaxDifficulty(Difficulty maxDifficulty) {
        this.maxDifficulty = maxDifficulty;
    }

    public String getAllowedTaskTypesJson() {
        return allowedTaskTypesJson;
    }

    public void setAllowedTaskTypesJson(String allowedTaskTypesJson) {
        this.allowedTaskTypesJson = allowedTaskTypesJson;
    }

    public int getMaxTasksPerSession() {
        return maxTasksPerSession;
    }

    public void setMaxTasksPerSession(int maxTasksPerSession) {
        this.maxTasksPerSession = maxTasksPerSession;
    }

    public boolean isSocraticEnabled() {
        return socraticEnabled;
    }

    public void setSocraticEnabled(boolean socraticEnabled) {
        this.socraticEnabled = socraticEnabled;
    }

    public int getMaxHintsPerTask() {
        return maxHintsPerTask;
    }

    public void setMaxHintsPerTask(int maxHintsPerTask) {
        this.maxHintsPerTask = maxHintsPerTask;
    }

    public long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(long updatedBy) {
        this.updatedBy = updatedBy;
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
