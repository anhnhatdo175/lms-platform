package com.example.lms.ai.profile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.ai.InteractionItemType;
import com.example.lms.ai.LearningInteraction;
import com.example.lms.ai.LearningInteractionRepository;
import com.example.lms.contract.ai.LearningInteractionRecorder;
import com.example.lms.contract.course.CourseQueryService;
import com.example.lms.exam.AttemptAnswer;
import com.example.lms.exam.AttemptAnswerRepository;
import com.example.lms.exam.AttemptQuestion;
import com.example.lms.exam.AttemptQuestionRepository;
import com.example.lms.exam.AttemptStatus;
import com.example.lms.exam.Exam;
import com.example.lms.exam.ExamAttempt;
import com.example.lms.exam.ExamAttemptRepository;
import com.example.lms.exam.ExamRepository;

/**
 * Điểm giao 6 phía C: ghi {@code learning_interactions} từ Attempt đã chấm rồi cập nhật hồ sơ. Idempotent theo
 * {@code event_key = EXAMQ:{attempt_question_id}}. Chỉ ghi câu đã được trả lời; câu bỏ trống không phải bằng chứng
 * (chưa làm ≠ sai, BR-AI-04). Chạy trong transaction riêng vì B gọi sau khi transaction chấm bài đã commit.
 */
@Service
public class LearningInteractionRecorderImpl implements LearningInteractionRecorder {

    private static final Logger log = LoggerFactory.getLogger(LearningInteractionRecorderImpl.class);

    private final ExamAttemptRepository attempts;
    private final ExamRepository exams;
    private final AttemptQuestionRepository questions;
    private final AttemptAnswerRepository answers;
    private final LearningInteractionRepository interactions;
    private final CourseQueryService courses;
    private final ProfileUpdater updater;

    public LearningInteractionRecorderImpl(ExamAttemptRepository attempts, ExamRepository exams,
            AttemptQuestionRepository questions, AttemptAnswerRepository answers,
            LearningInteractionRepository interactions, CourseQueryService courses, ProfileUpdater updater) {
        this.attempts = attempts;
        this.exams = exams;
        this.questions = questions;
        this.answers = answers;
        this.interactions = interactions;
        this.courses = courses;
        this.updater = updater;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordExamInteractions(long attemptId) {
        ExamAttempt attempt = attempts.findById(attemptId).orElse(null);
        if (attempt == null || attempt.getStatus() != AttemptStatus.GRADED) {
            return;
        }
        Exam exam = exams.findById(attempt.getExamId()).orElse(null);
        Long lessonId = exam == null ? null : exam.getLessonId();
        Long courseId = lessonId == null ? null
                : courses.findLesson(lessonId).map(CourseQueryService.LessonRef::courseId).orElse(null);
        Map<Long, AttemptAnswer> answerByQuestion = new HashMap<>();
        for (AttemptAnswer a : answers.findByAttemptId(attemptId)) {
            answerByQuestion.put(a.getAttemptQuestionId(), a);
        }
        List<AttemptQuestion> all = questions.findByAttemptIdOrderByPositionAsc(attemptId);
        for (AttemptQuestion q : all) {
            AttemptAnswer answer = answerByQuestion.get(q.getId());
            if (answer == null || answer.getAnsweredAt() == null) {
                continue;
            }
            String eventKey = "EXAMQ:" + q.getId();
            if (interactions.existsByEventKey(eventKey)) {
                continue;
            }
            LearningInteraction i = LearningInteraction.create(attempt.getStudentId(),
                    InteractionItemType.EXAM_QUESTION, q.getId(),
                    attempt.getGradedAt() != null ? attempt.getGradedAt() : answer.getAnsweredAt(), eventKey);
            i.setCourseId(courseId);
            i.setLessonId(lessonId);
            i.setSkillCode(q.getSkillCodeSnapshot());
            i.setSourceAttemptId(attemptId);
            i.setDifficultyLevel(q.getDifficultySnapshot());
            i.setScore(answer.getScoreAwarded() == null ? BigDecimal.ZERO : answer.getScoreAwarded());
            i.setMaxScore(q.getMaximumScore());
            i.setCorrect(answer.getCorrect());
            i.setHintsUsed(0);
            try {
                interactions.saveAndFlush(i);
            } catch (DataIntegrityViolationException duplicate) {
                // Ghi đồng thời cùng event_key: bản kia thắng, không ghi trùng.
                log.debug("interaction {} already recorded", eventKey);
                continue;
            }
            updater.apply(i);
        }
    }
}
