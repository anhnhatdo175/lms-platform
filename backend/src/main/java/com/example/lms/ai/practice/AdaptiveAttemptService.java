package com.example.lms.ai.practice;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public AdaptiveAttemptService(AdaptiveGeneratedTaskRepository tasks, AdaptiveTaskSessionRepository sessions,
            AdaptiveTaskAttemptRepository attempts, AdaptiveTaskHintEventRepository hints,
            LearningInteractionRepository interactions, LearnerProfileRepository profiles, ProfileUpdater updater,
            EnrollmentAccessService enrollments, AdaptiveSessionService sessionViews, Clock clock) {
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
    }

    @Transactional
    public Submitted submit(long studentId, long taskId, SubmitRequest request) {
        String text = validate(request);
        String eventId = UUID.fromString(request.clientEventId()).toString();

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
        attempt.setGradingMethod(GradingMethod.RULE_BASED);
        attempt.setGradedAt(now);
        if (grade.gradable()) {
            attempt.setCorrect(grade.correct());
            attempt.setScore(grade.correct() ? BigDecimal.ONE : BigDecimal.ZERO);
            attempt.setFeedbackText(grade.correct() ? "Chính xác." : "Chưa đúng.");
            attempt.setGradingConfidence(BigDecimal.ONE);
        } else {
            attempt.setFeedbackText("Câu trả lời đã được ghi nhận, cần giáo viên xem lại.");
        }
        try {
            attempts.saveAndFlush(attempt);
        } catch (DataIntegrityViolationException race) {
            // Hai yêu cầu đồng thời: UNIQUE(student, client_event_id) hoặc UNIQUE(task, student) chọn một người thắng.
            Optional<AdaptiveTaskAttempt> winner = attempts.findByStudentIdAndClientEventId(studentId, eventId);
            if (winner.isPresent()) {
                return new Submitted(duplicate(winner.get(), taskId), false);
            }
            throw new BusinessException(ErrorCode.AI_TASK_ALREADY_ATTEMPTED, "Task was already attempted");
        }

        Integer before = profiles.findByStudentId(studentId).map(p -> p.getProfileVersion()).orElse(null);
        Integer after = before;
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
            updater.apply(i);
            attempt.setInteractionId(i.getId());
            attempts.save(attempt);
            after = profiles.findByStudentId(studentId).map(p -> p.getProfileVersion()).orElse(null);
            updated = true;
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
                answer == null ? null : String.valueOf(answer), task.getExplanation(), profileUpdated, before, after,
                a.getSubmittedAt(), a.getHintsUsed(), duplicate);
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
