package com.example.lms.course;

import com.example.lms.exam.Exam;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "lesson_prerequisites")
public class LessonPrerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, unique = true, columnDefinition = "BIGINT UNSIGNED")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_lesson_id", columnDefinition = "BIGINT UNSIGNED")
    private Lesson prerequisiteLesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_exam_id", columnDefinition = "BIGINT UNSIGNED")
    private Exam prerequisiteExam;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 30)
    private PrerequisiteConditionType conditionType;

    protected LessonPrerequisite() {
    }

    public LessonPrerequisite(
            Lesson lesson,
            Lesson prerequisiteLesson,
            Exam prerequisiteExam,
            PrerequisiteConditionType conditionType) {
        this.lesson = lesson;
        this.prerequisiteLesson = prerequisiteLesson;
        this.prerequisiteExam = prerequisiteExam;
        this.conditionType = conditionType;
    }

    public void replace(
            Lesson prerequisiteLesson,
            Exam prerequisiteExam,
            PrerequisiteConditionType conditionType) {
        this.prerequisiteLesson = prerequisiteLesson;
        this.prerequisiteExam = prerequisiteExam;
        this.conditionType = conditionType;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public Lesson getPrerequisiteLesson() {
        return prerequisiteLesson;
    }

    public Exam getPrerequisiteExam() {
        return prerequisiteExam;
    }

    public PrerequisiteConditionType getConditionType() {
        return conditionType;
    }

    public LessonPrerequisiteRef toRef() {
        return new LessonPrerequisiteRef(
                prerequisiteLesson == null ? null : prerequisiteLesson.getId(),
                prerequisiteExam == null ? null : prerequisiteExam.getId(),
                conditionType.name());
    }
}
