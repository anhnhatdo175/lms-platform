package com.example.lms.question;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.example.lms.contract.question.Difficulty;
import com.example.lms.contract.question.QuestionType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Câu hỏi chính thức của Course (ERD 9.2). Sửa Question không đổi Attempt đã snapshot (Spec 14), nên {@code version}
 * chỉ để truy vết, không dùng làm khóa lạc quan.
 */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private Long courseId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(name = "skill_code", length = 150)
    private String skillCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private QuestionSource source;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QuestionOption> options = new ArrayList<>();

    protected Question() {
    }

    public Question(long courseId, String content, QuestionType questionType, Difficulty difficulty,
            String skillCode, QuestionSource source, String explanation, long createdBy, Instant now) {
        this.courseId = courseId;
        this.content = content;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.skillCode = skillCode;
        this.source = source;
        this.explanation = explanation;
        this.active = true;
        this.createdBy = createdBy;
        this.version = 1;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Bước 1 của việc thay toàn bộ Option (Spec: gửi {@code options} thì thay thế toàn bộ). Caller phải flush trước khi
     * gọi {@link #addOptions}, vì Hibernate chèn trước khi xóa và sẽ vi phạm UNIQUE(question_id, position).
     */
    public void clearOptions() {
        options.clear();
    }

    public void addOptions(List<ValidatedOption> validated, Instant now) {
        for (ValidatedOption v : validated) {
            options.add(new QuestionOption(this, v.content(), v.correct(), v.position(), now));
        }
    }

    public void update(String content, QuestionType type, Difficulty difficulty, String skillCode,
            boolean skillCodeProvided, String explanation, boolean explanationProvided, Boolean active, Instant now) {
        if (content != null) {
            this.content = content;
        }
        if (type != null) {
            this.questionType = type;
        }
        if (difficulty != null) {
            this.difficulty = difficulty;
        }
        if (skillCodeProvided) {
            this.skillCode = skillCode;
        }
        if (explanationProvided) {
            this.explanation = explanation;
        }
        if (active != null) {
            this.active = active;
        }
        this.version++;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        this.active = false;
        this.version++;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getContent() {
        return content;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getSkillCode() {
        return skillCode;
    }

    public QuestionSource getSource() {
        return source;
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean isActive() {
        return active;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<QuestionOption> getOptions() {
        return options;
    }

    /** Option đã qua kiểm tra, có {@code position} cuối cùng. */
    public record ValidatedOption(String content, boolean correct, int position) {
    }
}
