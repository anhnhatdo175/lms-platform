package com.example.lms.ai.profile;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.ai.AdaptivePracticeConfig;
import com.example.lms.ai.AdaptivePracticeConfigRepository;
import com.example.lms.ai.LearnerProfile;
import com.example.lms.ai.LearnerProfileRepository;
import com.example.lms.ai.LearnerSkillState;
import com.example.lms.ai.LearnerSkillStateRepository;
import com.example.lms.ai.LearningInteractionRepository;
import com.example.lms.ai.profile.LearnerProfileViews.ProfileView;
import com.example.lms.ai.profile.LearnerProfileViews.SkillView;
import com.example.lms.ai.session.JsonSupport;
import com.example.lms.ai.session.ZpdPolicy;
import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.contract.identity.AccessPolicy;
import com.example.lms.contract.question.Difficulty;
import com.example.lms.enrollment.EnrollmentAccessService;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Hồ sơ người học. Student xem toàn bộ hồ sơ của mình. Teacher chỉ xem học viên đã/đang ghi danh Course mình quản lý
 * và chỉ thấy kỹ năng thuộc phạm vi Course đó (Spec 7.4); thống kê toàn cục (điểm trung bình, tốc độ) không trả cho
 * Teacher vì gộp cả Course khác.
 */
@Service
public class LearnerProfileService {

    private final LearnerSkillStateRepository skills;
    private final LearnerProfileRepository profiles;
    private final LearningInteractionRepository interactions;
    private final AdaptivePracticeConfigRepository configs;
    private final AccessPolicy access;
    private final EnrollmentAccessService enrollments;
    private final Clock clock;

    public LearnerProfileService(LearnerSkillStateRepository skills, LearnerProfileRepository profiles,
            LearningInteractionRepository interactions, AdaptivePracticeConfigRepository configs,
            AccessPolicy access, EnrollmentAccessService enrollments, Clock clock) {
        this.skills = skills;
        this.profiles = profiles;
        this.interactions = interactions;
        this.configs = configs;
        this.access = access;
        this.enrollments = enrollments;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProfileView myProfile(long studentId) {
        return build(studentId, skills.findByStudentId(studentId), profiles.findByStudentId(studentId).orElse(null),
                true, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<SkillView> mySkills(long studentId) {
        return skillViews(skills.findByStudentId(studentId));
    }

    @Transactional(readOnly = true)
    public ProfileView studentProfileForTeacher(CurrentUser teacher, long studentId, long courseId) {
        if (!teacher.isAdmin() && !access.canManageCourse(teacher.id(), courseId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You do not manage this course");
        }
        if (!enrollments.hasEverEnrolled(studentId, courseId)) {
            throw BusinessException.notFound("Student");
        }
        Set<String> scope = new HashSet<>(interactions.findSkillCodesByStudentAndCourse(studentId, courseId));
        for (AdaptivePracticeConfig c : configs.findByCourseId(courseId)) {
            scope.addAll(JsonSupport.stringList(c.getAllowedSkillsJson()));
        }
        List<LearnerSkillState> scoped = skills.findByStudentId(studentId).stream()
                .filter(s -> scope.contains(s.getSkillCode())).toList();
        return build(studentId, scoped, profiles.findByStudentId(studentId).orElse(null), false, clock.instant());
    }

    // ---- ánh xạ thuần (có test) ----

    static ProfileView build(long studentId, List<LearnerSkillState> states, LearnerProfile profile,
            boolean includeGlobalStats, Instant now) {
        List<SkillView> views = skillViews(states);
        double sum = 0;
        int evidence = 0;
        List<String> strengths = new ArrayList<>();
        List<String> needs = new ArrayList<>();
        for (SkillView v : views) {
            sum += v.mastery();
            evidence += v.evidenceCount();
            if (!v.coldStart()) {
                if (v.mastery() >= ProfileMath.STRONG_AT) {
                    strengths.add(v.skillCode());
                } else if (v.mastery() < ProfileMath.WEAK_BELOW) {
                    needs.add(v.skillCode());
                }
            }
        }
        double overall = views.isEmpty() ? ZpdPolicy.COLD_START_MASTERY : sum / views.size();
        Difficulty recommended = ZpdPolicy.target(overall, evidence, Difficulty.EASY, Difficulty.HARD);
        Instant updated = profile != null && profile.getUpdatedAt() != null ? profile.getUpdatedAt() : now;
        return new ProfileView(studentId, profile == null ? 1 : Math.max(1, profile.getProfileVersion()),
                round(overall), recommended, strengths, needs, views, updated,
                includeGlobalStats && profile != null ? profile.getAverageScore() : null,
                includeGlobalStats && profile != null ? profile.getScoreStddev() : null,
                includeGlobalStats && profile != null ? profile.getLearningPace() : null);
    }

    static List<SkillView> skillViews(List<LearnerSkillState> states) {
        return states.stream().sorted(Comparator.comparing(LearnerSkillState::getSkillCode)).map(s -> {
            int evidence = s.getEvidenceCount();
            double mastery = ZpdPolicy.effectiveMastery(s.getMasteryScore().doubleValue(), evidence);
            return new SkillView(s.getSkillCode(), s.getSkillName(), round(mastery), evidence, s.getLastEvidenceAt(),
                    ZpdPolicy.target(mastery, evidence, Difficulty.EASY, Difficulty.HARD), s.getSubjectCode(),
                    ZpdPolicy.isColdStart(evidence));
        }).toList();
    }

    private static double round(double v) {
        return Math.round(v * 100000.0) / 100000.0;
    }
}
