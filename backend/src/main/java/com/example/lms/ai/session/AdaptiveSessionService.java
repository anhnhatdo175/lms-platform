package com.example.lms.ai.session;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.ai.AdaptiveGeneratedTask;
import com.example.lms.ai.AdaptiveGeneratedTaskRepository;
import com.example.lms.ai.AdaptivePracticeConfig;
import com.example.lms.ai.AdaptivePracticeConfigRepository;
import com.example.lms.ai.AdaptiveTaskSession;
import com.example.lms.ai.AdaptiveTaskSessionRepository;
import com.example.lms.ai.AdaptiveTaskType;
import com.example.lms.ai.AiKnowledgeDocumentRepository;
import com.example.lms.ai.SessionStatus;
import com.example.lms.ai.ValidationStatus;
import com.example.lms.ai.session.AdaptiveSessionViews.CreateRequest;
import com.example.lms.ai.session.AdaptiveSessionViews.Evidence;
import com.example.lms.ai.session.AdaptiveSessionViews.PracticeOptions;
import com.example.lms.ai.session.AdaptiveSessionViews.SessionView;
import com.example.lms.ai.session.AdaptiveSessionViews.TaskView;
import com.example.lms.contract.course.CourseQueryService;
import com.example.lms.enrollment.EnrollmentAccessService;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Phía Student của phiên nhiệm vụ thích ứng. Tạo phiên chỉ ghi PENDING (202); {@link AdaptiveSessionWorker} xử lý sau.
 * Mọi truy cập theo chủ sở hữu: phiên của người khác trả 404 (Spec 7.4).
 */
@Service
public class AdaptiveSessionService {

    private static final int MAX_TASK_COUNT = 10;

    private final AdaptiveTaskSessionRepository sessions;
    private final AdaptiveGeneratedTaskRepository tasks;
    private final AdaptivePracticeConfigRepository configs;
    private final AiKnowledgeDocumentRepository documents;
    private final SessionStore store;
    private final EnrollmentAccessService enrollments;
    private final CourseQueryService courses;
    private final Clock clock;

    public AdaptiveSessionService(AdaptiveTaskSessionRepository sessions, AdaptiveGeneratedTaskRepository tasks,
            AdaptivePracticeConfigRepository configs, AiKnowledgeDocumentRepository documents, SessionStore store,
            EnrollmentAccessService enrollments, CourseQueryService courses, Clock clock) {
        this.sessions = sessions;
        this.tasks = tasks;
        this.configs = configs;
        this.documents = documents;
        this.store = store;
        this.enrollments = enrollments;
        this.courses = courses;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PracticeOptions practiceOptions(long studentId, long courseId, Long lessonId) {
        enrollments.requireActiveAccess(studentId, courseId);
        requireLessonInCourse(courseId, lessonId);
        Optional<AdaptivePracticeConfig> config = resolveConfig(courseId, lessonId);
        if (config.isEmpty()) {
            return new PracticeOptions(false, false, courseId, lessonId, null, null, List.of(), null, null, List.of(),
                    null, false, null);
        }
        AdaptivePracticeConfig c = config.get();
        return new PracticeOptions(true, c.isEnabled(), courseId, lessonId, c.getSubjectCode(), c.getGradeLevel(),
                JsonSupport.stringList(c.getAllowedSkillsJson()), c.getMinDifficulty(), c.getMaxDifficulty(),
                taskTypes(c.getAllowedTaskTypesJson()), c.getMaxTasksPerSession(), c.isSocraticEnabled(),
                c.getMaxHintsPerTask());
    }

    @Transactional
    public Created create(long studentId, CreateRequest request) {
        validate(request);
        String requestId = UUID.fromString(request.clientRequestId()).toString();
        Optional<AdaptiveTaskSession> existing = sessions.findByStudentIdAndClientRequestId(studentId, requestId);
        if (existing.isPresent()) {
            return new Created(sameRequestOrConflict(existing.get(), request), false);
        }
        long courseId = request.courseId();
        enrollments.requireActiveAccess(studentId, courseId);
        requireLessonInCourse(courseId, request.lessonId());
        AdaptivePracticeConfig config = resolveConfig(courseId, request.lessonId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_NOT_CONFIGURED, "AI practice is not configured"));
        if (!config.isEnabled()) {
            throw new BusinessException(ErrorCode.AI_DISABLED, "AI practice is disabled for this course");
        }
        List<String> allowedSkills = JsonSupport.stringList(config.getAllowedSkillsJson());
        List<AdaptiveTaskType> allowedTypes = taskTypes(config.getAllowedTaskTypesJson());
        List<String> topics = request.targetTopics() == null ? List.of() : request.targetTopics();
        List<AdaptiveTaskType> types = request.taskTypes() == null || request.taskTypes().isEmpty() ? allowedTypes
                : request.taskTypes();
        if (request.taskCount() > config.getMaxTasksPerSession() || !allowedSkills.containsAll(topics)
                || !allowedTypes.containsAll(types)) {
            throw new BusinessException(ErrorCode.AI_REQUEST_OUT_OF_BOUNDS, "Request is outside the teacher's limits");
        }
        AdaptiveTaskSession session = AdaptiveTaskSession.create(studentId, courseId, request.lessonId(), requestId,
                clock.instant());
        session.setConfigId(config.getId());
        session.setTaskCount(request.taskCount());
        session.setConstraintsJson(constraintsSnapshot(config, allowedSkills, allowedTypes));
        session.setRequestedTopicsJson(JsonSupport.toJson(topics));
        session.setRequestedTaskTypesJson(JsonSupport.toJson(types.stream().map(Enum::name).toList()));
        try {
            sessions.saveAndFlush(session);
        } catch (DataIntegrityViolationException race) {
            // Hai yêu cầu cùng clientRequestId đồng thời: UNIQUE(student_id, client_request_id) chọn một người thắng.
            AdaptiveTaskSession winner = sessions.findByStudentIdAndClientRequestId(studentId, requestId)
                    .orElseThrow(() -> race);
            return new Created(sameRequestOrConflict(winner, request), false);
        }
        return new Created(view(session), true);
    }

    @Transactional(readOnly = true)
    public SessionView get(long studentId, long sessionId) {
        return view(owned(studentId, sessionId));
    }

    @Transactional(readOnly = true)
    public Page<SessionView> list(long studentId, Pageable pageable) {
        return sessions.findByStudentId(studentId, pageable).map(this::view);
    }

    @Transactional
    public SessionView retry(long studentId, long sessionId) {
        AdaptiveTaskSession s = owned(studentId, sessionId);
        if (s.getStatus() != SessionStatus.FAILED) {
            throw BusinessException.stateConflict("Only a FAILED session can be retried");
        }
        if (!SessionStateMachine.canManualRetry(s.getStatus(), s.getManualRetryCount())) {
            throw new BusinessException(ErrorCode.AI_RETRY_LIMIT_REACHED, "Manual retry limit reached");
        }
        if (!store.manualRetry(sessionId, SessionStateMachine.MAX_MANUAL_RETRIES)) {
            throw BusinessException.stateConflict("Session state changed, reload and try again");
        }
        return view(sessions.findById(sessionId).orElseThrow(() -> BusinessException.notFound("Session")));
    }

    public record Created(SessionView session, boolean created) {
    }

    // ---- nội bộ ----

    private AdaptiveTaskSession owned(long studentId, long sessionId) {
        return sessions.findById(sessionId).filter(s -> s.getStudentId() == studentId)
                .orElseThrow(() -> BusinessException.notFound("Session"));
    }

    private SessionView sameRequestOrConflict(AdaptiveTaskSession s, CreateRequest r) {
        boolean same = s.getCourseId() == r.courseId() && java.util.Objects.equals(s.getLessonId(), r.lessonId())
                && s.getTaskCount() == r.taskCount();
        if (!same) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                    "clientRequestId was already used with a different request");
        }
        return view(s);
    }

    private static void validate(CreateRequest r) {
        if (r == null || r.courseId() == null || r.courseId() < 1 || r.taskCount() == null || r.taskCount() < 1
                || r.taskCount() > MAX_TASK_COUNT || r.clientRequestId() == null
                || (r.lessonId() != null && r.lessonId() < 1)
               ) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "courseId, taskCount (1-10), clientRequestId required");
        }
        try {
            UUID.fromString(r.clientRequestId());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "clientRequestId must be a UUID");
        }
    }

    private void requireLessonInCourse(long courseId, Long lessonId) {
        if (lessonId != null && courses.findLesson(lessonId).filter(l -> l.courseId() == courseId).isEmpty()) {
            throw BusinessException.notFound("Lesson");
        }
    }

    private Optional<AdaptivePracticeConfig> resolveConfig(long courseId, Long lessonId) {
        if (lessonId != null) {
            Optional<AdaptivePracticeConfig> lessonConfig = configs.findByCourseIdAndLessonId(courseId, lessonId);
            if (lessonConfig.isPresent()) {
                return lessonConfig;
            }
        }
        return configs.findByCourseIdAndLessonIdIsNull(courseId);
    }

    private static List<AdaptiveTaskType> taskTypes(String json) {
        return JsonSupport.stringList(json).stream().map(AdaptiveTaskType::valueOf).toList();
    }

    private static String constraintsSnapshot(AdaptivePracticeConfig c, List<String> skills,
            List<AdaptiveTaskType> types) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("subjectCode", c.getSubjectCode());
        m.put("gradeLevel", c.getGradeLevel());
        m.put("allowedSkills", skills);
        m.put("minDifficulty", c.getMinDifficulty());
        m.put("maxDifficulty", c.getMaxDifficulty());
        m.put("allowedTaskTypes", types.stream().map(Enum::name).toList());
        m.put("socraticEnabled", c.isSocraticEnabled());
        m.put("maxHintsPerTask", c.getMaxHintsPerTask());
        return JsonSupport.toJson(m);
    }

    private SessionView view(AdaptiveTaskSession s) {
        List<TaskView> views = new ArrayList<>();
        if (s.getStatus() == SessionStatus.SUCCEEDED) {
            Map<String, Object> constraints = JsonSupport.object(s.getConstraintsJson());
            boolean socratic = Boolean.TRUE.equals(constraints.get("socraticEnabled"));
            for (AdaptiveGeneratedTask t : tasks.findBySessionIdOrderByPosition(s.getId())) {
                if (t.getValidationStatus() == ValidationStatus.VALID) {
                    views.add(taskView(t, socratic));
                }
            }
        }
        return new SessionView(s.getId(), s.getStatus(), s.getCourseId(), s.getLessonId(), s.getTaskCount(), views,
                s.getProfileVersion(), s.getPipelineVersion(), s.getErrorCode(), s.getCreatedAt(), s.getCompletedAt(),
                s.getConfigId(), JsonSupport.object(s.getConstraintsJson()), s.getAutoRetryCount(),
                s.getManualRetryCount(), s.getNextRetryAt());
    }

    private TaskView taskView(AdaptiveGeneratedTask t, boolean socratic) {
        Map<String, Object> content = JsonSupport.object(t.getContentJson());
        List<Evidence> evidence = new ArrayList<>();
        for (String id : JsonSupport.stringList(t.getEvidenceChunkIdsJson())) {
            try {
                long docId = Long.parseLong(id);
                documents.findById(docId).ifPresent(d -> evidence.add(new Evidence(docId, id, d.getTitle(), "")));
            } catch (NumberFormatException ignored) {
                // id không phải document id: bỏ qua thay vì lộ giá trị lạ ra API.
            }
        }
        int hints = socratic ? JsonSupport.stringList(t.getHintStepsJson()).size() : 0;
        return new TaskView(t.getId(), t.getTaskType(), String.valueOf(content.getOrDefault("prompt", "")), null,
                t.getDifficultyLevel(), List.of(t.getSkillCode()), evidence, t.getOrigin(),
                t.getPersonalizationReason() == null ? "" : t.getPersonalizationReason(), hints, false);
    }
}
