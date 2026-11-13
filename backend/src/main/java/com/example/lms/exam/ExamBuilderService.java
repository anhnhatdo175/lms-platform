package com.example.lms.exam;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.contract.course.CourseQueryService.LessonRef;
import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.contract.question.QuestionQueryService;
import com.example.lms.contract.question.QuestionQueryService.QuestionView;
import com.example.lms.exam.ExamLookup.ExamContext;
import com.example.lms.exam.dto.AddExamQuestionRequest;
import com.example.lms.exam.dto.CreateExamRequest;
import com.example.lms.exam.dto.ExamBuilderResponse;
import com.example.lms.exam.dto.ExamBuilderResponse.BuilderQuestion;
import com.example.lms.exam.dto.ExamBuilderResponse.TeacherQuestion;
import com.example.lms.exam.dto.ExamSummaryResponse;
import com.example.lms.exam.dto.UpdateExamRequest;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/** Exam builder của Teacher (OpenAPI {@code /teacher/exams*}); mọi thao tác yêu cầu {@code canManageCourse}. */
@Service
public class ExamBuilderService {

    private final ExamRepository exams;
    private final ExamQuestionRepository examQuestions;
    private final ExamAttemptRepository attempts;
    private final QuestionQueryService questions;
    private final ExamLookup lookup;
    private final Clock clock;

    public ExamBuilderService(ExamRepository exams, ExamQuestionRepository examQuestions,
            ExamAttemptRepository attempts, QuestionQueryService questions, ExamLookup lookup, Clock clock) {
        this.exams = exams;
        this.examQuestions = examQuestions;
        this.attempts = attempts;
        this.questions = questions;
        this.lookup = lookup;
        this.clock = clock;
    }

    @Transactional
    public ExamSummaryResponse create(CurrentUser user, long lessonId, CreateExamRequest request) {
        LessonRef lesson = lookup.lesson(lessonId);
        lookup.requireManage(user, lesson.courseId());
        if (!lesson.isExamLesson()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Exams can only be created on EXAM lessons");
        }
        if (exams.findByLessonId(lessonId).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Lesson " + lessonId + " already has an exam");
        }
        Exam exam = exams.save(new Exam(lessonId, request.title(), request.description(), request.durationMinutes(),
                request.passingScore(), request.maximumAttempts(), Boolean.TRUE.equals(request.shuffleQuestions()),
                Boolean.TRUE.equals(request.shuffleOptions()), request.resultDisplayMode(), request.startAt(),
                request.endAt(), user.id(), clock.instant()));
        return ExamSummaryResponse.from(exam, 0, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public ExamBuilderResponse get(CurrentUser user, long examId) {
        return builderView(lookup.forManager(user, examId).exam());
    }

    /** Cài đặt đổi được lúc nào cũng được; Attempt đã bắt đầu giữ hạn và điểm đã snapshot. */
    @Transactional
    public ExamSummaryResponse update(CurrentUser user, long examId, UpdateExamRequest r) {
        Exam exam = lookup.forManager(user, examId).exam();
        exam.applySettings(r.title() != null ? r.title() : exam.getTitle(),
                r.description() != null ? r.description() : exam.getDescription(),
                r.durationMinutes() != null ? r.durationMinutes() : exam.getDurationMinutes(),
                r.passingScore() != null ? r.passingScore() : exam.getPassingScore(),
                r.maximumAttempts() != null ? r.maximumAttempts() : exam.getMaximumAttempts(),
                r.shuffleQuestions() != null ? r.shuffleQuestions() : exam.isShuffleQuestions(),
                r.shuffleOptions() != null ? r.shuffleOptions() : exam.isShuffleOptions(),
                r.resultDisplayMode() != null ? r.resultDisplayMode() : exam.getResultDisplayMode(),
                r.startAt() != null ? r.startAt() : exam.getStartAt(), r.endAt() != null ? r.endAt() : exam.getEndAt(),
                clock.instant());
        return summary(exam);
    }

    @Transactional
    public ExamBuilderResponse addQuestion(CurrentUser user, long examId, AddExamQuestionRequest request) {
        ExamContext context = lookup.forManager(user, examId);
        Exam exam = context.exam();
        requireStructureEditable(exam);
        QuestionView question = questions.findQuestions(List.of(request.questionId())).stream().findFirst()
                .orElseThrow(() -> BusinessException.notFound("Question"));
        if (question.courseId() != context.courseId() || !QuestionRules.isGradable(question)) {
            throw new BusinessException(ErrorCode.QUESTION_INVALID,
                    "Question must belong to the same course, be active and have a valid answer");
        }
        List<ExamQuestion> current = examQuestions.findByExamIdOrderByPositionAsc(examId);
        if (current.stream().anyMatch(q -> q.getQuestionId() == request.questionId().longValue())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Question is already in the exam");
        }
        int position = request.position() != null ? request.position() : examQuestions.findMaxPosition(examId) + 1;
        if (current.stream().anyMatch(q -> q.getPosition() == position)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Position " + position + " is taken");
        }
        examQuestions.save(new ExamQuestion(examId, request.questionId(), position, request.score(),
                !Boolean.FALSE.equals(request.required()), clock.instant()));
        return builderView(exam);
    }

    @Transactional
    public void removeQuestion(CurrentUser user, long examId, long questionId) {
        Exam exam = lookup.forManager(user, examId).exam();
        requireStructureEditable(exam);
        ExamQuestion.Key key = new ExamQuestion.Key(examId, questionId);
        if (!examQuestions.existsById(key)) {
            throw BusinessException.notFound("Exam question");
        }
        examQuestions.deleteById(key);
    }

    /** DRAFT → PUBLISHED; cần ít nhất một câu chấm được và tổng điểm lớn hơn 0. */
    @Transactional
    public ExamSummaryResponse publish(CurrentUser user, long examId) {
        ExamContext context = lookup.forManager(user, examId);
        List<ExamQuestion> rows = examQuestions.findByExamIdOrderByPositionAsc(examId);
        BigDecimal total = rows.stream().map(ExamQuestion::getScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<QuestionView> found = questions.findQuestions(rows.stream().map(ExamQuestion::getQuestionId).toList());
        boolean allGradable = found.size() == rows.size() && found.stream()
                .allMatch(q -> q.courseId() == context.courseId() && QuestionRules.isGradable(q));
        if (rows.isEmpty() || total.signum() <= 0 || !allGradable) {
            throw new BusinessException(ErrorCode.EXAM_NOT_READY,
                    "Exam needs at least one valid question and a total score above 0");
        }
        context.exam().publish(clock.instant());
        return ExamSummaryResponse.from(context.exam(), rows.size(), total);
    }

    /** PUBLISHED → CLOSED; không nhận Attempt mới. */
    @Transactional
    public ExamSummaryResponse close(CurrentUser user, long examId) {
        Exam exam = lookup.forManager(user, examId).exam();
        exam.close(clock.instant());
        return summary(exam);
    }

    /** Thêm, bỏ câu chỉ khi Exam chưa đóng và chưa có Attempt (OpenAPI removeExamQuestion). */
    private void requireStructureEditable(Exam exam) {
        if (exam.getStatus() == ExamStatus.CLOSED || attempts.existsByExamId(exam.getId())) {
            throw BusinessException.stateConflict("Exam questions cannot change after attempts have started");
        }
    }

    private ExamSummaryResponse summary(Exam exam) {
        List<ExamQuestion> rows = examQuestions.findByExamIdOrderByPositionAsc(exam.getId());
        return ExamSummaryResponse.from(exam, rows.size(),
                rows.stream().map(ExamQuestion::getScore).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private ExamBuilderResponse builderView(Exam exam) {
        List<ExamQuestion> rows = examQuestions.findByExamIdOrderByPositionAsc(exam.getId());
        Map<Long, QuestionView> byId = questions.findQuestions(rows.stream().map(ExamQuestion::getQuestionId).toList())
                .stream().collect(Collectors.toMap(QuestionView::id, Function.identity()));
        List<BuilderQuestion> items = rows.stream().filter(row -> byId.containsKey(row.getQuestionId()))
                .map(row -> new BuilderQuestion(row.getQuestionId(), row.getPosition(), row.getScore(),
                        row.isRequired(), TeacherQuestion.from(byId.get(row.getQuestionId()))))
                .toList();
        return new ExamBuilderResponse(summary(exam), items);
    }

}
