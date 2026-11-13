package com.example.lms.exam;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.exam.grader.AttemptGrading;
import com.example.lms.exam.grader.QuestionGraderResolver;
import com.example.lms.exam.snapshot.OptionSnapshotCodec;

/**
 * Đường chấm duy nhất, dùng chung cho nộp bài, hết hạn lười (khi Student gọi bất kỳ API nào của Attempt đã quá
 * hạn) và job hết giờ. Bên gọi phải đang giữ khóa {@code FOR UPDATE} của Attempt.
 */
@Component
public class AttemptFinalizer {

    private final AttemptQuestionRepository attemptQuestions;
    private final AttemptAnswerRepository answers;
    private final AttemptAnswerOptionRepository answerOptions;
    private final ExamRepository exams;
    private final QuestionGraderResolver graders;
    private final ApplicationEventPublisher events;

    public AttemptFinalizer(AttemptQuestionRepository attemptQuestions, AttemptAnswerRepository answers,
            AttemptAnswerOptionRepository answerOptions, ExamRepository exams, QuestionGraderResolver graders,
            ApplicationEventPublisher events) {
        this.attemptQuestions = attemptQuestions;
        this.answers = answers;
        this.answerOptions = answerOptions;
        this.exams = exams;
        this.graders = graders;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void gradeAndClose(ExamAttempt attempt, SubmissionType type, Instant now) {
        List<AttemptQuestion> snapshot = attemptQuestions.findByAttemptIdOrderByPositionAsc(attempt.getId());
        Map<Long, AttemptAnswer> answerByQuestion = answers.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(AttemptAnswer::getAttemptQuestionId, Function.identity()));
        Map<Long, Set<Long>> selections = selections(answerByQuestion.values());

        AttemptGrading.Result result = AttemptGrading.grade(
                snapshot.stream().map(q -> new AttemptGrading.QuestionSnapshot(q.getId(), q.getQuestionType(),
                        q.getMaximumScore(), OptionSnapshotCodec.read(q.getOptionSnapshotJson()))).toList(),
                selections, attempt.getMaximumScore(), attempt.getPassingScore(), graders);

        for (AttemptQuestion question : snapshot) {
            // Ghi cả câu bỏ trống để thống kê có đủ mẫu số
            AttemptAnswer answer = answerByQuestion.computeIfAbsent(question.getId(),
                    id -> answers.save(new AttemptAnswer(attempt.getId(), id, now)));
            AttemptGrading.QuestionResult graded = result.byAttemptQuestion().get(question.getId());
            answer.grade(graded.correct(), graded.scoreAwarded(), now);
        }
        attempt.grade(type, result.score(), result.passed(), now);

        long lessonId = exams.findById(attempt.getExamId()).map(Exam::getLessonId).orElseThrow();
        events.publishEvent(new ExamGradedEvent(attempt.getId(), attempt.getStudentId(), attempt.getExamId(), lessonId,
                result.passed(), result.score(), type));
    }

    /** Option đã chọn theo {@code attempt_question_id}. */
    Map<Long, Set<Long>> selections(Collection<AttemptAnswer> attemptAnswers) {
        Map<Long, Long> questionByAnswer = attemptAnswers.stream()
                .collect(Collectors.toMap(AttemptAnswer::getId, AttemptAnswer::getAttemptQuestionId));
        Map<Long, Set<Long>> selected = new HashMap<>();
        if (questionByAnswer.isEmpty()) {
            return selected;
        }
        answerOptions.findByAttemptAnswerIdIn(questionByAnswer.keySet()).forEach(option -> selected
                .computeIfAbsent(questionByAnswer.get(option.getAttemptAnswerId()), k -> new HashSet<>())
                .add(option.getOptionId()));
        return selected;
    }
}
