package com.example.lms.exam;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.lms.contract.course.LessonAccessService;
import com.example.lms.contract.course.LessonAccessService.LessonAccess;
import com.example.lms.contract.question.QuestionType;
import com.example.lms.enrollment.Enrollment;
import com.example.lms.enrollment.EnrollmentAccessService;
import com.example.lms.exam.ExamLookup.ExamContext;
import com.example.lms.exam.dto.ExamAnswerAck;
import com.example.lms.exam.dto.ExamAnswerRequest;
import com.example.lms.exam.dto.ExamAttemptResponse;
import com.example.lms.exam.dto.ExamAttemptSessionResponse;
import com.example.lms.exam.dto.ExamAttemptSessionResponse.AttemptQuestionView;
import com.example.lms.exam.dto.ExamAttemptSessionResponse.OptionView;
import com.example.lms.exam.dto.ExamResultResponse;
import com.example.lms.exam.dto.ExamResultResponse.AnswerResult;
import com.example.lms.exam.dto.ExamViewResponse;
import com.example.lms.exam.snapshot.OptionSnapshot;
import com.example.lms.exam.snapshot.OptionSnapshotCodec;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Student làm bài (Spec 14.4–14.8). {@code expiresAt} của server là nguồn sự thật duy nhất: mọi API chạm vào một
 * Attempt đã quá hạn đều chốt nó thành GRADED + AUTO_EXPIRED trước, job hết giờ chỉ là lưới an toàn.
 * Attempt của người khác trả 404 (Spec 7.4).
 */
@Service
public class AttemptService {

    private final ExamAttemptRepository attempts;
    private final AttemptQuestionRepository attemptQuestions;
    private final AttemptAnswerRepository answers;
    private final AttemptAnswerOptionRepository answerOptions;
    private final AttemptSnapshotService snapshots;
    private final AttemptFinalizer finalizer;
    private final ExamLookup lookup;
    private final EnrollmentAccessService enrollmentAccess;
    private final LessonAccessService lessonAccess;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public AttemptService(ExamAttemptRepository attempts,
            AttemptQuestionRepository attemptQuestions, AttemptAnswerRepository answers,
            AttemptAnswerOptionRepository answerOptions, AttemptSnapshotService snapshots, AttemptFinalizer finalizer,
            ExamLookup lookup, EnrollmentAccessService enrollmentAccess, LessonAccessService lessonAccess,
            TransactionTemplate transactions, Clock clock) {
        this.attempts = attempts;
        this.attemptQuestions = attemptQuestions;
        this.answers = answers;
        this.answerOptions = answerOptions;
        this.snapshots = snapshots;
        this.finalizer = finalizer;
        this.lookup = lookup;
        this.enrollmentAccess = enrollmentAccess;
        this.lessonAccess = lessonAccess;
        this.transactions = transactions;
        this.clock = clock;
    }

    /** {@code GET /learning/exams/{id}}: thông tin trước khi thi, không có câu hỏi. */
    public ExamViewResponse learningExam(long studentId, long examId) {
        return transactions.execute(status -> {
            Instant now = clock.instant();
            ExamContext context = lookup.forLearner(examId);
            enrollmentAccess.requireActiveAccess(studentId, context.courseId());
            closeExpiredInProgress(studentId, examId, now);
            Exam exam = context.exam();
            List<ExamAttempt> mine = attempts.findByExamIdAndStudentIdOrderByAttemptNumberAsc(examId, studentId);
            Long inProgressId = mine.stream().filter(ExamAttempt::isInProgress).map(ExamAttempt::getId).findFirst()
                    .orElse(null);
            boolean scoreVisible = visibility(exam, now).score();
            List<ExamAttempt> graded = mine.stream().filter(a -> a.getStatus() == AttemptStatus.GRADED).toList();
            BigDecimal best = scoreVisible ? graded.stream().map(ExamAttempt::getScore).filter(Objects::nonNull)
                    .max(Comparator.naturalOrder()).orElse(null) : null;
            Boolean passed = scoreVisible && !graded.isEmpty()
                    ? graded.stream().anyMatch(a -> Boolean.TRUE.equals(a.getPassed())) : null;
            boolean canStart = inProgressId == null && exam.isOpenAt(now)
                    && (exam.getMaximumAttempts() == null || mine.size() < exam.getMaximumAttempts())
                    && lessonAccess.checkOpen(studentId, context.lesson().id()).open();
            return new ExamViewResponse(exam.getId(), exam.getLessonId(), exam.getTitle(), exam.getDescription(),
                    exam.getDurationMinutes(), exam.getPassingScore(), exam.getMaximumAttempts(), mine.size(),
                    exam.getResultDisplayMode(), exam.getStartAt(), exam.getEndAt(), canStart, inProgressId, best,
                    passed);
        });
    }

    /** @param created {@code false} khi trả lại Attempt IN_PROGRESS đang có (HTTP 200 thay vì 201) */
    public record StartResult(ExamAttemptSessionResponse session, boolean created) {
    }

    public StartResult start(long studentId, long examId) {
        try {
            return transactions.execute(status -> doStart(studentId, examId));
        } catch (DataIntegrityViolationException raced) {
            // Hai lần bắt đầu song song tính cùng attempt_number: UNIQUE chặn bên sau, trả Attempt của bên trước
            return transactions.execute(status -> attempts
                    .findIdByExamIdAndStudentIdAndStatus(examId, studentId, AttemptStatus.IN_PROGRESS)
                    .flatMap(attempts::findById)
                    .map(existing -> new StartResult(session(existing, lookup.forLearner(examId).exam()), false)))
                    .orElseThrow(() -> raced);
        }
    }

    private StartResult doStart(long studentId, long examId) {
        Instant now = clock.instant();
        ExamContext context = lookup.forLearner(examId);
        Exam exam = context.exam();
        Enrollment enrollment = enrollmentAccess.requireActiveAccess(studentId, context.courseId());
        LessonAccess access = lessonAccess.checkOpen(studentId, context.lesson().id());
        if (!access.open()) {
            ErrorCode reason = access.lockReason() != null ? access.lockReason() : ErrorCode.FORBIDDEN;
            throw new BusinessException(reason, 403, "Lesson is locked");
        }
        Optional<ExamAttempt> current = lockInProgress(studentId, examId);
        if (current.isPresent()) {
            ExamAttempt attempt = current.get();
            if (!attempt.isExpiredAt(now)) {
                return new StartResult(session(attempt, exam), false);
            }
            finalizer.gradeAndClose(attempt, SubmissionType.AUTO_EXPIRED, now);
        }
        if (!exam.isOpenAt(now)) {
            boolean closed = exam.getStatus() == ExamStatus.CLOSED
                    || (exam.getEndAt() != null && !now.isBefore(exam.getEndAt()));
            throw new BusinessException(closed ? ErrorCode.EXAM_CLOSED : ErrorCode.EXAM_NOT_OPEN,
                    closed ? "Exam is closed" : "Exam is not open yet");
        }
        if (exam.getMaximumAttempts() != null
                && attempts.countByExamIdAndStudentId(examId, studentId) >= exam.getMaximumAttempts()) {
            throw new BusinessException(ErrorCode.EXAM_ATTEMPT_LIMIT_REACHED, "No attempts left");
        }
        AttemptSnapshotService.Plan plan = snapshots.plan(exam, context.courseId());
        ExamAttempt attempt = attempts.saveAndFlush(ExamAttempt.start(examId, studentId, enrollment.getId(),
                attempts.findMaxAttemptNumber(examId, studentId) + 1, now,
                AttemptWindow.expiresAt(now, exam.getDurationMinutes(), exam.getEndAt()), plan.maximumScore(),
                exam.getPassingScore()));
        snapshots.write(attempt, plan, exam.isShuffleQuestions(), exam.isShuffleOptions(), now);
        return new StartResult(session(attempt, exam), true);
    }

    public ExamAttemptSessionResponse attempt(long studentId, long attemptId) {
        return transactions.execute(status -> {
            ExamAttempt attempt = lockOwned(studentId, attemptId);
            closeIfExpired(attempt, clock.instant());
            return session(attempt, lookup.forLearner(attempt.getExamId()).exam());
        });
    }

    /**
     * Autosave (Spec 14.5): ghi đè đáp án của một câu. Quá hạn thì chốt Attempt (đã commit) rồi mới trả
     * {@code ATTEMPT_EXPIRED}: nếu chốt và ném lỗi trong cùng transaction, việc chốt sẽ bị rollback theo.
     */
    public ExamAnswerAck saveAnswer(long studentId, long attemptId, long attemptQuestionId, ExamAnswerRequest request) {
        Optional<ExamAnswerAck> saved = transactions.execute(status -> {
            Instant now = clock.instant();
            ExamAttempt attempt = lockOwned(studentId, attemptId);
            if (!attempt.isInProgress()) {
                throw new BusinessException(ErrorCode.ATTEMPT_NOT_IN_PROGRESS, "Attempt is already submitted");
            }
            if (attempt.isExpiredAt(now)) {
                finalizer.gradeAndClose(attempt, SubmissionType.AUTO_EXPIRED, now);
                return Optional.empty();
            }
            if (request.version() != null && request.version() != attempt.getVersion()) {
                throw new BusinessException(ErrorCode.CONFLICT, "Attempt was changed elsewhere; reload it");
            }
            AttemptQuestion question = attemptQuestions.findByIdAndAttemptId(attemptQuestionId, attemptId)
                    .orElseThrow(() -> BusinessException.notFound("Attempt question"));
            Map<Long, String> options = OptionSnapshotCodec.read(question.getOptionSnapshotJson()).stream()
                    .collect(Collectors.toMap(OptionSnapshot::id, OptionSnapshot::content));
            Set<Long> selected = new LinkedHashSet<>(request.selectedOptionIds());
            if (!options.keySet().containsAll(selected)
                    || (question.getQuestionType() == QuestionType.SINGLE_CHOICE && selected.size() > 1)) {
                throw new BusinessException(ErrorCode.QUESTION_INVALID, "Selected options do not fit this question");
            }
            AttemptAnswer answer = answers.findByAttemptIdAndAttemptQuestionId(attemptId, attemptQuestionId)
                    .orElseGet(() -> answers.save(new AttemptAnswer(attemptId, attemptQuestionId, now)));
            answerOptions.deleteByAttemptAnswerId(answer.getId());
            for (Long optionId : selected) {
                answerOptions.save(new AttemptAnswerOption(answer.getId(), optionId, options.get(optionId), now));
            }
            answer.markAnswered(now);
            attempt.bumpVersion();
            return Optional.of(new ExamAnswerAck(attemptQuestionId, List.copyOf(selected), now, attempt.getVersion()));
        });
        return saved.orElseThrow(() -> new BusinessException(ErrorCode.ATTEMPT_EXPIRED,
                "Time is up; the attempt was submitted with the saved answers"));
    }

    /**
     * Nộp bài: idempotent theo trạng thái (đã GRADED thì trả kết quả cũ). Quá hạn thì chấm AUTO_EXPIRED theo đáp án
     * đã autosave; request nộp không mang đáp án nên nộp trễ không sửa được gì (AC-EXAM-03).
     */
    public ExamAttemptResponse submit(long studentId, long attemptId) {
        return transactions.execute(status -> {
            Instant now = clock.instant();
            ExamAttempt attempt = lockOwned(studentId, attemptId);
            if (attempt.isInProgress()) {
                finalizer.gradeAndClose(attempt,
                        attempt.isExpiredAt(now) ? SubmissionType.AUTO_EXPIRED : SubmissionType.MANUAL, now);
            }
            Exam exam = lookup.forLearner(attempt.getExamId()).exam();
            return ExamAttemptResponse.from(attempt, now, visibility(exam, now).score());
        });
    }

    public ExamResultResponse result(long studentId, long attemptId) {
        return transactions.execute(status -> {
            Instant now = clock.instant();
            ExamAttempt attempt = lockOwned(studentId, attemptId);
            closeIfExpired(attempt, now);
            if (attempt.getStatus() != AttemptStatus.GRADED) {
                throw BusinessException.stateConflict("Attempt has not been submitted yet");
            }
            Exam exam = lookup.forLearner(attempt.getExamId()).exam();
            ResultVisibility visible = visibility(exam, now);
            return new ExamResultResponse(attempt.getId(), attempt.getExamId(), attempt.getStatus(),
                    attempt.getSubmissionType(), visible.score() ? attempt.getScore() : null,
                    attempt.getMaximumScore(), attempt.getPassingScore(), visible.score() ? attempt.getPassed() : null,
                    attempt.getSubmittedAt(), exam.getResultDisplayMode(),
                    visible.answers() ? answerResults(attempt) : null);
        });
    }

    private List<AnswerResult> answerResults(ExamAttempt attempt) {
        Map<Long, AttemptAnswer> byQuestion = answers.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(AttemptAnswer::getAttemptQuestionId, Function.identity()));
        Map<Long, Set<Long>> selected = finalizer.selections(byQuestion.values());
        return attemptQuestions.findByAttemptIdOrderByPositionAsc(attempt.getId()).stream().map(question -> {
            AttemptAnswer answer = byQuestion.get(question.getId());
            List<Long> correctIds = OptionSnapshotCodec.read(question.getOptionSnapshotJson()).stream()
                    .filter(OptionSnapshot::correct).map(OptionSnapshot::id).toList();
            return new AnswerResult(question.getId(), List.copyOf(selected.getOrDefault(question.getId(), Set.of())),
                    correctIds, answer == null ? null : answer.getCorrect(),
                    answer == null ? null : answer.getScoreAwarded(), question.getExplanationSnapshot());
        }).toList();
    }

    /** Đề cho Student: chỉ nội dung và Option, không có cờ đúng hay giải thích. */
    private ExamAttemptSessionResponse session(ExamAttempt attempt, Exam exam) {
        Instant now = clock.instant();
        Map<Long, AttemptAnswer> byQuestion = answers.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(AttemptAnswer::getAttemptQuestionId, Function.identity()));
        Map<Long, Set<Long>> selected = finalizer.selections(byQuestion.values());
        List<AttemptQuestionView> questions = attemptQuestions.findByAttemptIdOrderByPositionAsc(attempt.getId())
                .stream()
                .map(q -> new AttemptQuestionView(q.getId(), q.getPosition(), q.getQuestionType(),
                        q.getContentSnapshot(),
                        OptionSnapshotCodec.read(q.getOptionSnapshotJson()).stream()
                                .map(o -> new OptionView(o.id(), o.content())).toList(),
                        q.getMaximumScore(), List.copyOf(selected.getOrDefault(q.getId(), Set.of()))))
                .toList();
        return new ExamAttemptSessionResponse(ExamAttemptResponse.from(attempt, now, visibility(exam, now).score()),
                questions);
    }

    private Optional<ExamAttempt> lockInProgress(long studentId, long examId) {
        return attempts.findIdByExamIdAndStudentIdAndStatus(examId, studentId, AttemptStatus.IN_PROGRESS)
                .flatMap(attempts::findByIdForUpdate);
    }

    private void closeExpiredInProgress(long studentId, long examId, Instant now) {
        lockInProgress(studentId, examId).ifPresent(attempt -> closeIfExpired(attempt, now));
    }

    private void closeIfExpired(ExamAttempt attempt, Instant now) {
        if (attempt.isInProgress() && attempt.isExpiredAt(now)) {
            finalizer.gradeAndClose(attempt, SubmissionType.AUTO_EXPIRED, now);
        }
    }

    /** Khóa đầu tiên trong transaction là khóa trên chính Attempt, nên luôn đọc được bản mới nhất. */
    private ExamAttempt lockOwned(long studentId, long attemptId) {
        return attempts.findByIdForUpdate(attemptId).filter(a -> a.getStudentId() == studentId)
                .orElseThrow(() -> BusinessException.notFound("Attempt"));
    }

    private static ResultVisibility visibility(Exam exam, Instant now) {
        boolean closed = exam.getStatus() == ExamStatus.CLOSED
                || (exam.getEndAt() != null && !now.isBefore(exam.getEndAt()));
        return ResultVisibility.of(exam.getResultDisplayMode(), closed);
    }

}
