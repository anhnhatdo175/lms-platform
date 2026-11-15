package com.example.lms.ai.config;

import java.time.Clock;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.ai.AdaptivePracticeConfig;
import com.example.lms.ai.AdaptivePracticeConfigRepository;
import com.example.lms.ai.AdaptiveTaskType;
import com.example.lms.ai.config.PracticeConfigViews.Request;
import com.example.lms.ai.config.PracticeConfigViews.View;
import com.example.lms.ai.session.JsonSupport;
import com.example.lms.contract.audit.AuditLogService;
import com.example.lms.contract.course.CourseQueryService;
import com.example.lms.contract.identity.AccessPolicy;
import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Cấu hình luyện tập AI của Teacher: một cấu hình mặc định cho Course, và có thể một cấu hình ghi đè cho từng Lesson
 * (Lesson thắng Course khi Student tạo phiên). Chỉ Teacher quản lý Course (hoặc Admin) được đọc/ghi.
 */
@Service
public class PracticeConfigService {

    static final int DEFAULT_MAX_TASKS = 5;
    static final int DEFAULT_MAX_HINTS = 3;
    static final int MAX_SKILL_LENGTH = 150;

    private final AdaptivePracticeConfigRepository configs;
    private final AccessPolicy access;
    private final CourseQueryService courses;
    private final AuditLogService audit;
    private final Clock clock;

    public PracticeConfigService(AdaptivePracticeConfigRepository configs, AccessPolicy access,
            CourseQueryService courses, AuditLogService audit, Clock clock) {
        this.configs = configs;
        this.access = access;
        this.courses = courses;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public View getForCourse(CurrentUser user, long courseId) {
        requireManage(user, courseId);
        return view(configs.findByCourseIdAndLessonIdIsNull(courseId).orElseThrow(PracticeConfigService::notConfigured),
                null);
    }

    @Transactional
    public View upsertForCourse(CurrentUser user, long courseId, Request request) {
        requireManage(user, courseId);
        return upsert(user, courseId, null, request);
    }

    @Transactional(readOnly = true)
    public View getForLesson(CurrentUser user, long lessonId) {
        long courseId = courseOfLesson(lessonId);
        requireManage(user, courseId);
        AdaptivePracticeConfig override = configs.findByCourseIdAndLessonId(courseId, lessonId)
                .orElseThrow(PracticeConfigService::notConfigured);
        return view(override, configs.findByCourseIdAndLessonIdIsNull(courseId).orElse(null));
    }

    @Transactional
    public View upsertForLesson(CurrentUser user, long lessonId, Request request) {
        long courseId = courseOfLesson(lessonId);
        requireManage(user, courseId);
        return upsert(user, courseId, lessonId, request);
    }

    @Transactional
    public void deleteForLesson(CurrentUser user, long lessonId) {
        long courseId = courseOfLesson(lessonId);
        requireManage(user, courseId);
        AdaptivePracticeConfig override = configs.findByCourseIdAndLessonId(courseId, lessonId)
                .orElseThrow(PracticeConfigService::notConfigured);
        configs.delete(override);
        audit.record(new AuditLogService.AuditEntry(user.id(), "AI_CONFIG_LESSON_RESET", "ADAPTIVE_PRACTICE_CONFIG",
                override.getId(), snapshot(override), null, null));
    }

    // ---- nội bộ ----

    private View upsert(CurrentUser user, long courseId, Long lessonId, Request r) {
        validate(r);
        Optional<AdaptivePracticeConfig> existing = lessonId == null
                ? configs.findByCourseIdAndLessonIdIsNull(courseId)
                : configs.findByCourseIdAndLessonId(courseId, lessonId);
        String before = existing.map(PracticeConfigService::snapshot).orElse(null);
        AdaptivePracticeConfig c = existing.orElseGet(() -> AdaptivePracticeConfig.create(courseId, lessonId,
                clock.instant()));
        c.setEnabled(r.enabled());
        c.setSubjectCode(blankToNull(r.subjectCode()));
        c.setGradeLevel(blankToNull(r.gradeLevel()));
        c.setAllowedSkillsJson(JsonSupport.toJson(r.allowedSkills().stream().map(String::strip).distinct().toList()));
        c.setMinDifficulty(r.minDifficulty());
        c.setMaxDifficulty(r.maxDifficulty());
        c.setAllowedTaskTypesJson(JsonSupport.toJson(r.allowedTaskTypes().stream().distinct().map(Enum::name)
                .toList()));
        c.setMaxTasksPerSession(r.maxTasksPerSession() == null ? DEFAULT_MAX_TASKS : r.maxTasksPerSession());
        c.setSocraticEnabled(Boolean.TRUE.equals(r.socraticEnabled()));
        c.setMaxHintsPerTask(r.maxHintsPerTask() == null ? DEFAULT_MAX_HINTS : r.maxHintsPerTask());
        c.setUpdatedBy(user.id());
        c.setUpdatedAt(clock.instant());
        try {
            configs.saveAndFlush(c);
        } catch (DataIntegrityViolationException race) {
            // UNIQUE(course_id, scope_key): hai Teacher tạo cùng lúc; người sau thử lại sẽ thành cập nhật.
            throw new BusinessException(ErrorCode.CONFLICT, "Configuration was changed concurrently; retry");
        }
        audit.record(new AuditLogService.AuditEntry(user.id(),
                lessonId == null ? "AI_CONFIG_COURSE_UPSERT" : "AI_CONFIG_LESSON_UPSERT", "ADAPTIVE_PRACTICE_CONFIG",
                c.getId(), before, snapshot(c), null));
        AdaptivePracticeConfig courseDefault = lessonId == null ? null
                : configs.findByCourseIdAndLessonIdIsNull(courseId).orElse(null);
        return view(c, courseDefault);
    }

    /** 422 theo OpenAPI: thiếu trường bắt buộc, danh sách rỗng, min > max, ngoài khoảng cho phép. */
    static void validate(Request r) {
        if (r == null || r.enabled() == null || r.allowedSkills() == null || r.allowedSkills().isEmpty()
                || r.minDifficulty() == null || r.maxDifficulty() == null || r.allowedTaskTypes() == null
                || r.allowedTaskTypes().isEmpty()) {
            throw invalid("enabled, allowedSkills, minDifficulty, maxDifficulty and allowedTaskTypes are required");
        }
        Set<String> seen = new HashSet<>();
        for (String skill : r.allowedSkills()) {
            if (skill == null || skill.isBlank() || skill.strip().length() > MAX_SKILL_LENGTH) {
                throw invalid("allowedSkills must contain non-blank skill codes of at most 150 characters");
            }
            seen.add(skill.strip());
        }
        if (seen.isEmpty()) {
            throw invalid("allowedSkills must not be empty");
        }
        if (r.allowedTaskTypes().stream().anyMatch(java.util.Objects::isNull)) {
            throw invalid("allowedTaskTypes must not contain null");
        }
        if (r.minDifficulty().ordinal() > r.maxDifficulty().ordinal()) {
            throw invalid("minDifficulty must not exceed maxDifficulty");
        }
        if (r.maxTasksPerSession() != null && (r.maxTasksPerSession() < 1 || r.maxTasksPerSession() > 10)) {
            throw invalid("maxTasksPerSession must be between 1 and 10");
        }
        if (r.maxHintsPerTask() != null && (r.maxHintsPerTask() < 1 || r.maxHintsPerTask() > 5)) {
            throw invalid("maxHintsPerTask must be between 1 and 5");
        }
    }

    private long courseOfLesson(long lessonId) {
        return courses.findLesson(lessonId).map(CourseQueryService.LessonRef::courseId)
                .orElseThrow(() -> BusinessException.notFound("Lesson"));
    }

    private void requireManage(CurrentUser user, long courseId) {
        if (!user.isAdmin() && !access.canManageCourse(user.id(), courseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You do not manage this course");
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, 422, message);
    }

    private static BusinessException notConfigured() {
        return new BusinessException(ErrorCode.AI_NOT_CONFIGURED, 404, "AI practice is not configured");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    private static String snapshot(AdaptivePracticeConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", c.isEnabled());
        m.put("allowedSkills", JsonSupport.stringList(c.getAllowedSkillsJson()));
        m.put("minDifficulty", c.getMinDifficulty());
        m.put("maxDifficulty", c.getMaxDifficulty());
        m.put("allowedTaskTypes", JsonSupport.stringList(c.getAllowedTaskTypesJson()));
        m.put("maxTasksPerSession", c.getMaxTasksPerSession());
        m.put("socraticEnabled", c.isSocraticEnabled());
        m.put("maxHintsPerTask", c.getMaxHintsPerTask());
        return JsonSupport.toJson(m);
    }

    /** {@code fallback} là cấu hình Course khi {@code c} là ghi đè Lesson: môn/khối trống thì kế thừa. */
    static View view(AdaptivePracticeConfig c, AdaptivePracticeConfig fallback) {
        String subject = c.getSubjectCode() != null ? c.getSubjectCode()
                : fallback == null ? null : fallback.getSubjectCode();
        String grade = c.getGradeLevel() != null ? c.getGradeLevel()
                : fallback == null ? null : fallback.getGradeLevel();
        List<AdaptiveTaskType> types = JsonSupport.stringList(c.getAllowedTaskTypesJson()).stream()
                .map(AdaptiveTaskType::valueOf).toList();
        return new View(c.getId(), c.getCourseId(), c.getLessonId(), c.isEnabled(), c.getSubjectCode(),
                c.getGradeLevel(), JsonSupport.stringList(c.getAllowedSkillsJson()), c.getMinDifficulty(),
                c.getMaxDifficulty(), types, c.getMaxTasksPerSession(), c.isSocraticEnabled(), c.getMaxHintsPerTask(),
                subject, grade, c.getUpdatedBy(), c.getUpdatedAt());
    }
}
