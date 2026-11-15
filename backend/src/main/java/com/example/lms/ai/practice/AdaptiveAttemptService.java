package com.example.lms.ai.practice;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.lms.ai.AdaptiveGeneratedTask;
import com.example.lms.ai.AdaptiveGeneratedTaskRepository;
import com.example.lms.ai.AdaptiveTaskAttempt;
import com.example.lms.ai.AdaptiveTaskAttemptRepository;
import com.example.lms.ai.AdaptiveTaskHintEventRepository;
import com.example.lms.ai.AdaptiveTaskSession;
import com.example.lms.ai.AdaptiveTaskSessionRepository;
import com.example.lms.ai.AdaptiveTaskType;
import com.example.lms.ai.GradingMethod;
import com.example.lms.ai.InteractionItemType;
import com.example.lms.ai.LearnerProfileRepository;
import com.example.lms.ai.LearningInteraction;
import com.example.lms.ai.LearningInteractionRepository;
import com.example.lms.ai.SessionStatus;
import com.example.lms.ai.ValidationStatus;
import com.example.lms.ai.practice.AttemptViews.AnswerPayload;
import com.example.lms.ai.practice.AttemptViews.AttemptHistory;
import com.example.lms.ai.practice.AttemptViews.AttemptResult;
import com.example.lms.ai.practice.AttemptViews.SubmitRequest;
import com.example.lms.ai.practice.AttemptViews.Submitted;
import com.example.lms.ai.profile.ProfileUpdater;
import com.example.lms.ai.session.AdaptiveSessionService;
import com.example.lms.ai.session.JsonSupport;
import com.example.lms.enrollment.EnrollmentAccessService;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Nộp bài luyện tập: chấm rule-based, ghi attempt + interaction và cập nhật hồ sơ (EMA) trong MỘT transaction.
 * Idempotent theo {@code clientEventId}; mỗi học viên chỉ nộp một lần cho một task. Đáp án và giải thích chỉ trả sau
 * khi nộp (AC-AI-12). Task không tự chấm được (loại đáp án cần giáo viên) được ghi nhận nhưng KHÔNG cập nhật hồ sơ.
 */
@Service
public class AdaptiveAttemptService {

    private final AdaptiveGeneratedTaskRepository tasks;
    private final AdaptiveTaskSessionRepository sessions;
    private final AdaptiveTaskAttemptRepository attempts;
    private final AdaptiveTaskHintEventRepository hints;
    private final LearningInteractionRepository interactions;
    private final LearnerProfileRepository profiles;
    private final ProfileUpdater updater;
    private final EnrollmentAccessService enrollments;
    private final AdaptiveSessionService sessionViews;
    private final Clock clock;
    private final TransactionTemplate tx;

    public AdaptiveAttemptService(AdaptiveGeneratedTaskRepository tasks, AdaptiveTaskSessionRepository sessions,
            AdaptiveTaskAttemptRepository attempts, AdaptiveTaskHintEventRepository hints,
            LearningInteractionRepository interactions, LearnerProfileRepository profiles, ProfileUpdater updater,
            EnrollmentAccessService enrollments, AdaptiveSessionService sessionViews, Clock clock,
            PlatformTransactionManager transactionManager) {
        this.tasks = tasks;
        this.sessions = sessions;
        this.attempts = attempts;
        this.hints = hints;
        this.interactions = interactions;
        this.profiles = profiles;
        this.updater = updater;
        this.enrollments = enrollments;
        this.sessionViews = sessionViews;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /**
     * Hai yêu cầu đồng thời bị UNIQUE(student, client_event_id) hoặc UNIQUE(task, student) chặn ở lúc flush. Transaction
     * đó đã hỏng (rollback-only) nên kết quả trùng được đọc lại trong một transaction MỚI, không bắt lỗi tại chỗ.
     */
    public Submitted submit(long studentId, long taskId, SubmitRequest request) {
        String text = validate(request);
        String eventId = UUID.fromString(request.clientEventId()).toString();
        try {
            return tx.execute(status -> doSubmit(studentId, taskId, request, text, eventId));
        } catch (DataIntegrityViolationException race) {
            return tx.execute(status -> attempts.findByStudentIdAndClientEventId(studentId, eventId)
                    .map(winner -> new Submitted(duplicate(winner, taskId), false))
                    .orElseThrow(() -> new BusinessException(ErrorCode.AI_TASK_ALREADY_ATTEMPTED,
                            "Task was already attempted")));
        }
    }

    private Submitted doSubmit(long studentId, long taskId, SubmitRequest request, String text, String eventId) {

        Optional<AdaptiveTaskAttempt> sameEvent = attempts.findByStudentIdAndClientEventId(studentId, eventId);
        if (sameEvent.isPresent()) {
            return new Submitted(duplicate(sameEvent.get(), taskId), false);
        }
        AdaptiveGeneratedTask task = tasks.findById(taskId).orElseThrow(() -> BusinessException.notFound("Task"));
        AdaptiveTaskSession session = sessions.findById(task.getSessionId())
                .filter(s -> s.getStudentId() == studentId).orElseThrow(() -> BusinessException.notFound("Task"));
        if (session.getStatus() != SessionStatus.SUCCEEDED || task.getValidationStatus() != ValidationStatus.VALID) {
            throw BusinessException.notFound("Task");
        }
        enrollments.requireActiveAccess(studentId, session.getCourseId());
        if (task.getTaskType() != AdaptiveTaskType.SHORT_ANSWER) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, 422, "This task type cannot be graded yet");
        }
        if (attempts.findByTaskIdAndStudentId(taskId, studentId).isPresent()) {
            throw new BusinessException(ErrorCode.AI_TASK_ALREADY_ATTEMPTED, "Task was already attempted");
        }

        Map<String, Object> key = JsonSupport.object(task.getAnswerKeyJson());
        RuleBasedGrader.Grade grade = RuleBasedGrader.grade((String) key.get("responseType"), key.get("answer"), text);
        int hintsUsed = (int) hints.countByTaskIdAndStudentId(taskId, studentId);
        var now = clock.instant();

        AdaptiveTaskAttempt attempt = AdaptiveTaskAttempt.create(taskId, studentId, eventId, now);
        attempt.setAnswerJson(JsonSupport.toJson(Map.of("text", text)));
        attempt.setMaxScore(BigDecimal.ONE);
        attempt.setHintsUsed(hintsUsed);
        attempt.setResponseTimeMs(request.responseTimeMs());
        if (grade.gradable()) {
            attempt.setGradingMethod(GradingMethod.RULE_BASED);
            attempt.setGradedAt(now);
            attempt.setCorrect(grade.correct());
            attempt.setScore(grade.correct() ? BigDecimal.ONE : BigDecimal.ZERO);
            attempt.setFeedbackText(grade.correct() ? "Chính xác." : "Chưa đúng.");
            attempt.setGradingConfidence(BigDecimal.ONE);
        } else {
            // Chưa chấm: chờ giáo viên, nên không có gradedAt.
            attempt.setGradingMethod(GradingMethod.TEACHER);
            attempt.setFeedbackText("Câu trả lời đã được ghi nhận, cần giáo viên xem lại.");
        }
        attempts.saveAndFlush(attempt);

        Integer before = null;
        Integer after = null;
        boolean updated = false;
        if (grade.gradable()) {
            LearningInteraction i = LearningInteraction.create(studentId, InteractionItemType.ADAPTIVE_TASK, taskId,
                    now, "TASK:" + attempt.getId());
            i.setCourseId(session.getCourseId());
            i.setLessonId(session.getLessonId());
            i.setSkillCode(task.getSkillCode());
            i.setDifficultyLevel(task.getDifficultyLevel());
            i.setScore(attempt.getScore());
            i.setMaxScore(BigDecimal.ONE);
            i.setCorrect(attempt.getCorrect());
            i.setResponseTimeMs(request.responseTimeMs());
            i.setHintsUsed(hintsUsed);
            interactions.saveAndFlush(i);
            ProfileUpdater.Applied applied = updater.apply(i);
            attempt.setInteractionId(i.getId());
            attempts.save(attempt);
            before = applied.versionBefore();
            after = applied.versionAfter();
            updated = applied.applied();
        } else {
            // Không tự chấm được: hồ sơ không đổi, báo phiên bản hiện tại cho cả hai trường.
            before = profiles.findByStudentId(studentId).map(p -> p.getProfileVersion()).orElse(null);
            after = before;
        }
        return new Submitted(result(attempt, task, updated, before, after, false), true);
    }

    @Transactional(readOnly = true)
    public List<AttemptHistory> history(long studentId, long taskId) {
        AdaptiveGeneratedTask task = tasks.findById(taskId).orElseThrow(() -> BusinessException.notFound("Task"));
        AdaptiveTaskSession session = sessions.findById(task.getSessionId())
                .filter(s -> s.getStudentId() == studentId).orElseThrow(() -> BusinessException.notFound("Task"));
        return attempts.findByTaskIdAndStudentId(taskId, studentId)
                .map(a -> new AttemptHistory(result(a, task, a.getInteractionId() != null, null, null, false),
                        sessionViews.publicTask(task, session)))
                .map(List::of).orElse(List.of());
    }

    // ---- nội bộ ----

    private AttemptResult duplicate(AdaptiveTaskAttempt a, long requestedTaskId) {
        if (a.getTaskId() != requestedTaskId) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                    "clientEventId was already used for a different task");
        }
        AdaptiveGeneratedTask task = tasks.findById(a.getTaskId()).orElseThrow(
                () -> BusinessException.notFound("Task"));
        return result(a, task, a.getInteractionId() != null, null, null, true);
    }

    private static AttemptResult result(AdaptiveTaskAttempt a, AdaptiveGeneratedTask task, boolean profileUpdated,
            Integer before, Integer after, boolean duplicate) {
        Object answer = JsonSupport.object(task.getAnswerKeyJson()).get("answer");
        return new AttemptResult(a.getId(), a.getTaskId(), a.getCorrect(),
                a.getScore() == null ? 0.0 : a.getScore().doubleValue(), a.getFeedbackText(),
                displayAnswer(answer), task.getExplanation(), profileUpdated, before, after,
                a.getSubmittedAt(), a.getHintsUsed(), duplicate);
    }

    /** Số nguyên không kèm ".0", số thập phân bỏ số 0 thừa; chuỗi giữ nguyên. */
    static String displayAnswer(Object answer) {
        if (answer == null) {
            return null;
        }
        if (answer instanceof Number n) {
            return new BigDecimal(n.toString()).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(answer);
    }

    /** @return văn bản đáp án đã kiểm tra hợp lệ */
    static String validate(SubmitRequest r) {
        if (r == null || r.answer() == null || r.responseTimeMs() == null || r.responseTimeMs() < 0
                || r.clientEventId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "answer, responseTimeMs, clientEventId required");
        }
        try {
            UUID.fromString(r.clientEventId());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "clientEventId must be a UUID");
        }
        AnswerPayload a = r.answer();
        boolean hasText = a.text() != null && !a.text().isBlank();
        boolean hasOption = a.selectedOptionId() != null && !a.selectedOptionId().isBlank();
        if (hasText == hasOption) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, 422,
                    "answer must contain exactly one of text or selectedOptionId");
        }
        if (!hasText) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, 422, "This task expects a text answer");
        }
        if (a.text().length() > 2000) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, 422, "text is too long");
        }
        return a.text();
    }
}
