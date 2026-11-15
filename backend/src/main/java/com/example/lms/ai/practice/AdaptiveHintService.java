package com.example.lms.ai.practice;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.ai.AdaptiveGeneratedTask;
import com.example.lms.ai.AdaptiveGeneratedTaskRepository;
import com.example.lms.ai.AdaptiveTaskAttemptRepository;
import com.example.lms.ai.AdaptiveTaskHintEvent;
import com.example.lms.ai.AdaptiveTaskHintEventRepository;
import com.example.lms.ai.AdaptiveTaskSession;
import com.example.lms.ai.AdaptiveTaskSessionRepository;
import com.example.lms.ai.SessionStatus;
import com.example.lms.ai.TaskOrigin;
import com.example.lms.ai.ValidationStatus;
import com.example.lms.ai.session.JsonSupport;
import com.example.lms.enrollment.EnrollmentAccessService;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Gợi ý Socratic (SHOULD, BR-AI-15). Gợi ý được sinh sẵn cùng task nên không gọi AI service; server đếm số lần cấp
 * trong {@code adaptive_task_hint_events} (không ghi vào learning_interactions). Idempotent theo Idempotency-Key.
 */
@Service
public class AdaptiveHintService {

    public record HintView(long taskId, int hintIndex, String hint, int hintsUsed, int hintsRemaining,
            boolean duplicate) {
    }

    private final AdaptiveGeneratedTaskRepository tasks;
    private final AdaptiveTaskSessionRepository sessions;
    private final AdaptiveTaskAttemptRepository attempts;
    private final AdaptiveTaskHintEventRepository events;
    private final EnrollmentAccessService enrollments;
    private final Clock clock;

    public AdaptiveHintService(AdaptiveGeneratedTaskRepository tasks, AdaptiveTaskSessionRepository sessions,
            AdaptiveTaskAttemptRepository attempts, AdaptiveTaskHintEventRepository events,
            EnrollmentAccessService enrollments, Clock clock) {
        this.tasks = tasks;
        this.sessions = sessions;
        this.attempts = attempts;
        this.events = events;
        this.enrollments = enrollments;
        this.clock = clock;
    }

    @Transactional
    public HintView request(long studentId, long taskId, String idempotencyKey) {
        String key = HintPolicy.validateKey(idempotencyKey);
        AdaptiveGeneratedTask task = tasks.findById(taskId).orElseThrow(() -> BusinessException.notFound("Task"));

        Optional<AdaptiveTaskHintEvent> replay = events.findByStudentIdAndIdempotencyKey(studentId, key);
        if (replay.isPresent()) {
            return replayed(replay.get(), task, studentId);
        }
        AdaptiveTaskSession session = sessions.findById(task.getSessionId())
                .filter(s -> s.getStudentId() == studentId).orElseThrow(() -> BusinessException.notFound("Task"));
        if (session.getStatus() != SessionStatus.SUCCEEDED || task.getValidationStatus() != ValidationStatus.VALID) {
            throw BusinessException.notFound("Task");
        }
        enrollments.requireActiveAccess(studentId, session.getCourseId());
        if (task.getOrigin() != TaskOrigin.GENERATED) {
            throw new BusinessException(ErrorCode.AI_HINT_NOT_AVAILABLE, 422, "Hints are only for generated tasks");
        }
        Map<String, Object> constraints = JsonSupport.object(session.getConstraintsJson());
        if (!Boolean.TRUE.equals(constraints.get("socraticEnabled"))) {
            throw new BusinessException(ErrorCode.AI_SOCRATIC_DISABLED, 422, "Socratic hints are disabled");
        }
        if (attempts.findByTaskIdAndStudentId(taskId, studentId).isPresent()) {
            throw new BusinessException(ErrorCode.AI_TASK_ALREADY_ATTEMPTED, "Task was already attempted");
        }
        List<String> steps = JsonSupport.stringList(task.getHintStepsJson());
        int max = ((Number) constraints.getOrDefault("maxHintsPerTask", 3)).intValue();
        long used = events.countByTaskIdAndStudentId(taskId, studentId);
        int index = HintPolicy.nextIndex(steps, max, used);
        try {
            events.saveAndFlush(AdaptiveTaskHintEvent.create(taskId, studentId, key, index, clock.instant()));
        } catch (DataIntegrityViolationException race) {
            // Yêu cầu song song: cùng key thì phát lại, khác key thì báo xung đột để client gọi lại.
            Optional<AdaptiveTaskHintEvent> winner = events.findByStudentIdAndIdempotencyKey(studentId, key);
            if (winner.isPresent()) {
                return replayed(winner.get(), task, studentId);
            }
            throw new BusinessException(ErrorCode.CONFLICT, "Concurrent hint request, please retry");
        }
        int usedNow = index;
        return new HintView(taskId, index, steps.get(index - 1), usedNow,
                HintPolicy.remaining(steps, max, usedNow), false);
    }

    private HintView replayed(AdaptiveTaskHintEvent e, AdaptiveGeneratedTask task, long studentId) {
        if (e.getTaskId() != task.getId()) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                    "Idempotency-Key was already used for a different task");
        }
        AdaptiveTaskSession session = sessions.findById(task.getSessionId()).orElseThrow(
                () -> BusinessException.notFound("Task"));
        List<String> steps = JsonSupport.stringList(task.getHintStepsJson());
        int max = ((Number) JsonSupport.object(session.getConstraintsJson()).getOrDefault("maxHintsPerTask", 3))
                .intValue();
        int used = (int) events.countByTaskIdAndStudentId(task.getId(), studentId);
        return new HintView(task.getId(), e.getHintIndex(), steps.get(e.getHintIndex() - 1), used,
                HintPolicy.remaining(steps, max, used), true);
    }
}
