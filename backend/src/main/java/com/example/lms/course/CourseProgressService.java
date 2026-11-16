package com.example.lms.course;

import com.example.lms.contract.course.CourseProgressQuery;
import com.example.lms.contract.course.LessonAccessService;
import com.example.lms.enrollment.Enrollment;
import com.example.lms.enrollment.EnrollmentRepository;
import com.example.lms.exam.ExamGradedEvent;
import com.example.lms.exam.ExamRepository;
import com.example.lms.exam.ExamResultQueryService;
import com.example.lms.shared.exception.ErrorCode;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CourseProgressService implements CourseProgressQuery, LessonAccessService {

    private final CourseRepository courses;
    private final CourseSectionRepository sections;
    private final LessonRepository lessons;
    private final LessonPrerequisiteRepository prerequisites;
    private final LessonProgressRepository progress;
    private final CourseCompletionRepository completions;
    private final EnrollmentRepository enrollments;
    private final ExamRepository exams;
    private final ExamResultQueryService examResults;
    private final Clock clock;

    public CourseProgressService(
            CourseRepository courses,
            CourseSectionRepository sections,
            LessonRepository lessons,
            LessonPrerequisiteRepository prerequisites,
            LessonProgressRepository progress,
            CourseCompletionRepository completions,
            EnrollmentRepository enrollments,
            ExamRepository exams,
            ExamResultQueryService examResults,
            Clock clock) {
        this.courses = courses;
        this.sections = sections;
        this.lessons = lessons;
        this.prerequisites = prerequisites;
        this.progress = progress;
        this.completions = completions;
        this.enrollments = enrollments;
        this.exams = exams;
        this.examResults = examResults;
        this.clock = clock;
    }

    @Override
    public LessonAccess checkOpen(long studentId, long lessonId) {
        Lesson lesson = lessons.findById(lessonId).orElse(null);
        if (lesson == null) return new LessonAccess(false, ErrorCode.LESSON_NOT_PUBLISHED, null);
        CourseSection section = lesson.getSection();
        Course course = section.getCourse();
        if (!lesson.isPublished() || !section.isPublished()
                || !(course.getStatus() == CourseStatus.PUBLISHED
                        || course.getStatus() == CourseStatus.HIDDEN
                        || course.getStatus() == CourseStatus.ARCHIVED)) {
            return new LessonAccess(false, ErrorCode.LESSON_NOT_PUBLISHED, null);
        }

        Optional<LessonPrerequisite> prerequisite = prerequisites.findByLesson_Id(lessonId);
        if (prerequisite.isPresent() && !prerequisiteSatisfied(studentId, prerequisite.get())) {
            return new LessonAccess(false, ErrorCode.LESSON_LOCKED_BY_PREREQUISITE, null);
        }

        Instant releaseAt = releaseAt(studentId, course.getId(), section);
        if (releaseAt != null && clock.instant().isBefore(releaseAt)) {
            return new LessonAccess(false, ErrorCode.LESSON_LOCKED_UNTIL_RELEASE, releaseAt);
        }
        return LessonAccess.opened();
    }

    @Override
    public Map<Long, BigDecimal> progressPercents(long studentId, Collection<Long> courseIds) {
        if (courseIds.isEmpty()) return Map.of();
        Map<Long, BigDecimal> result = new HashMap<>();
        Instant now = clock.instant();
        for (Long courseId : courseIds) {
            result.put(courseId, calculateProgress(studentId, courseId, now).progressPercent());
        }
        return result;
    }

    @Override
    public MyCourseOutline outline(long studentId, long courseId) {
        Course course = courses.findById(courseId).orElse(null);
        if (course == null) return new MyCourseOutline(null, List.of());

        Instant now = clock.instant();
        ProgressView courseProgress = calculateProgress(studentId, courseId, now);
        Map<Long, LessonProgress> progressByLesson = progress.findAllByStudentIdAndCourseId(studentId, courseId)
                .stream()
                .collect(Collectors.toMap(LessonProgress::getLessonId, item -> item, (first, ignored) -> first));
        List<MySection> sectionViews = new ArrayList<>();
        for (CourseSection section : sections.findAllByCourse_IdAndPublishedTrueOrderByPositionAscIdAsc(courseId)) {
            Instant releaseAt = releaseAt(studentId, courseId, section);
            List<MyLesson> lessonViews = lessons
                    .findAllBySection_IdAndPublishedTrueOrderByPositionAscIdAsc(section.getId())
                    .stream()
                    .map(lesson -> {
                        LessonProgress lessonProgress = progressByLesson.get(lesson.getId());
                        LessonAccess access = checkOpen(studentId, lesson.getId());
                        LessonPrerequisiteRef prerequisite = prerequisites.findByLesson_Id(lesson.getId())
                                .map(LessonPrerequisite::toRef)
                                .orElse(null);
                        return new MyLesson(
                                lesson.getId(),
                                lesson.getTitle(),
                                lesson.getLessonType(),
                                lesson.getPosition(),
                                lesson.getDurationSeconds(),
                                lesson.isMandatory(),
                                lessonProgress == null
                                        ? LessonProgressStatus.NOT_STARTED
                                        : lessonProgress.getStatus(),
                                lessonProgress == null ? null : lessonProgress.getCompletedAt(),
                                new LessonAccessView(
                                        access.open() ? "AVAILABLE"
                                                : access.lockReason() == ErrorCode.LESSON_LOCKED_BY_PREREQUISITE
                                                        ? "LOCKED_BY_PREREQUISITE"
                                                        : "LOCKED_UNTIL_RELEASE",
                                        access.releaseAt(),
                                        prerequisite));
                    })
                    .toList();
            sectionViews.add(new MySection(section.getId(), section.getTitle(), section.getPosition(),
                    releaseAt, lessonViews));
        }
        return new MyCourseOutline(courseProgress, sectionViews);
    }

    @Transactional
    public LessonProgress markLessonOpened(long studentId, Lesson lesson) {
        Instant now = clock.instant();
        LessonProgress lessonProgress = getOrCreateProgress(studentId, lesson, now);
        lessonProgress.touch(now);
        if (lesson.getCompletionMethod() == CompletionMethod.VIEW_CONTENT) {
            lessonProgress.complete(LessonCompletionSource.STUDENT_MANUAL, now);
            recordCourseCompletionIfEligible(studentId, lesson.getSection().getCourse(), now);
        }
        return progress.saveAndFlush(lessonProgress);
    }

    public ProgressView courseProgress(long studentId, long courseId) {
        if (!courses.existsById(courseId)) {
            throw com.example.lms.shared.exception.BusinessException.notFound("Course");
        }
        return calculateProgress(studentId, courseId, clock.instant());
    }

    @Transactional
    public HeartbeatResult heartbeat(long studentId, Lesson lesson, int positionSeconds, String clientEventId) {
        if (lesson.getLessonType() != LessonType.VIDEO
                || lesson.getCompletionMethod() != CompletionMethod.VIDEO_PERCENT
                || lesson.getDurationSeconds() == null
                || positionSeconds > lesson.getDurationSeconds()) {
            throw new com.example.lms.auth.AuthException(
                    "INVALID_LESSON_HEARTBEAT",
                    "Vị trí phát video không hợp lệ cho bài học này.",
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Instant now = clock.instant();
        LessonProgress lessonProgress = getOrCreateProgress(studentId, lesson, now);
        if (lessonProgress.isDuplicateHeartbeat(clientEventId)) {
            return heartbeatResult(lesson, lessonProgress, true);
        }
        if (lessonProgress.getStatus() == LessonProgressStatus.COMPLETED) {
            return heartbeatResult(lesson, lessonProgress, false);
        }
        lessonProgress.recordHeartbeat(clientEventId, positionSeconds, lesson.getDurationSeconds(), now);
        if (lesson.getCompletionThreshold() != null
                && lessonProgress.getProgressPercent().compareTo(lesson.getCompletionThreshold()) >= 0) {
            lessonProgress.complete(LessonCompletionSource.SYSTEM_VIDEO, now);
        }
        progress.saveAndFlush(lessonProgress);
        recordCourseCompletionIfEligible(studentId, lesson.getSection().getCourse(), now);
        return heartbeatResult(lesson, lessonProgress, false);
    }

    @Transactional
    public CompleteResult completeManually(long studentId, Lesson lesson, String clientEventId) {
        if (lesson.getCompletionMethod() != CompletionMethod.MANUAL
                && lesson.getCompletionMethod() != CompletionMethod.VIEW_CONTENT) {
            throw new com.example.lms.auth.AuthException(
                    "LESSON_COMPLETION_NOT_ALLOWED",
                    "Bài học này không hỗ trợ hoàn thành thủ công.",
                    org.springframework.http.HttpStatus.CONFLICT);
        }
        Instant now = clock.instant();
        LessonProgress lessonProgress = getOrCreateProgress(studentId, lesson, now);
        if (!lessonProgress.isDuplicateCompletion(clientEventId)) {
            lessonProgress.complete(LessonCompletionSource.STUDENT_MANUAL, now, clientEventId);
        }
        progress.saveAndFlush(lessonProgress);
        recordCourseCompletionIfEligible(studentId, lesson.getSection().getCourse(), now);
        return new CompleteResult(lesson.getId(), lessonProgress.getStatus(), lessonProgress.getCompletedAt());
    }

    @EventListener
    @Transactional
    public void onExamGraded(ExamGradedEvent event) {
        Lesson lesson = lessons.findById(event.lessonId()).orElse(null);
        if (lesson == null) return;
        boolean eligible = lesson.getCompletionMethod() == CompletionMethod.SUBMIT_EXAM
                || lesson.getCompletionMethod() == CompletionMethod.PASS_EXAM && event.passed();
        if (!eligible) return;

        Instant now = clock.instant();
        LessonProgress lessonProgress = getOrCreateProgress(event.studentId(), lesson, now);
        lessonProgress.complete(LessonCompletionSource.SYSTEM_EXAM, now);
        progress.saveAndFlush(lessonProgress);
        recordCourseCompletionIfEligible(event.studentId(), lesson.getSection().getCourse(), now);
    }

    private boolean prerequisiteSatisfied(long studentId, LessonPrerequisite prerequisite) {
        if (prerequisite.getConditionType() == PrerequisiteConditionType.COMPLETE_LESSON) {
            return progress.existsByStudentIdAndLessonIdAndStatus(
                    studentId,
                    prerequisite.getPrerequisiteLesson().getId(),
                    LessonProgressStatus.COMPLETED);
        }
        return examResults.hasPassed(studentId, prerequisite.getPrerequisiteExam().getId());
    }

    private Instant releaseAt(long studentId, long courseId, CourseSection section) {
        if (section.getReleaseAfterDays() == 0) return null;
        return enrollments.findByStudentIdAndCourseId(studentId, courseId).stream()
                .map(Enrollment::getStartsAt)
                .min(Instant::compareTo)
                .map(startedAt -> startedAt.plusSeconds(section.getReleaseAfterDays() * 86_400L))
                .orElse(null);
    }

    private ProgressView calculateProgress(long studentId, long courseId, Instant now) {
        List<LessonProgress> studentProgress = progress.findAllByStudentIdAndCourseId(studentId, courseId);
        Map<Long, LessonProgress> completedByLesson = studentProgress.stream()
                .collect(Collectors.toMap(LessonProgress::getLessonId, item -> item, (first, ignored) -> first));
        List<Lesson> releasedMandatory = lessons.findAllByCourseIdOrderBySectionAndPosition(courseId).stream()
                .filter(Lesson::isPublished)
                .filter(Lesson::isMandatory)
                .filter(lesson -> lesson.getSection().isPublished())
                .filter(lesson -> {
                    Instant releaseAt = releaseAt(studentId, courseId, lesson.getSection());
                    return releaseAt == null || !now.isBefore(releaseAt);
                })
                .toList();
        int completed = (int) releasedMandatory.stream()
                .filter(lesson -> completedByLesson.containsKey(lesson.getId())
                        && completedByLesson.get(lesson.getId()).getStatus() == LessonProgressStatus.COMPLETED)
                .count();
        int total = releasedMandatory.size();
        BigDecimal percent = total == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(completed * 100L).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        Instant lastAccessedAt = studentProgress.stream()
                .map(LessonProgress::getLastAccessedAt)
                .filter(value -> value != null)
                .max(Instant::compareTo)
                .orElse(null);
        Instant completedAt = completions.findByStudentIdAndCourseId(studentId, courseId)
                .map(CourseCompletion::getCompletedAt)
                .orElse(null);
        return new ProgressView(courseId, total, completed, percent, lastAccessedAt, completedAt);
    }

    private void recordCourseCompletionIfEligible(long studentId, Course course, Instant now) {
        if (course.getCompletionRule() == CourseCompletionRule.MANUAL_BY_TEACHER
                || completions.findByStudentIdAndCourseId(studentId, course.getId()).isPresent()) {
            return;
        }
        ProgressView current = calculateProgress(studentId, course.getId(), now);
        if (current.totalLessons() == 0 || current.completedLessons() != current.totalLessons()) return;

        if (course.getCompletionRule() == CourseCompletionRule.ALL_MANDATORY_LESSONS_AND_PASS_REQUIRED_EXAMS) {
            boolean everyRequiredExamPassed = lessons.findAllByCourseIdOrderBySectionAndPosition(course.getId())
                    .stream()
                    .filter(Lesson::isPublished)
                    .filter(Lesson::isMandatory)
                    .filter(lesson -> lesson.getLessonType() == LessonType.EXAM)
                    .allMatch(lesson -> exams.findByLessonId(lesson.getId())
                            .map(exam -> examResults.hasPassed(studentId, exam.getId()))
                            .orElse(false));
            if (!everyRequiredExamPassed) return;
        }

        completions.save(new CourseCompletion(
                studentId,
                course.getId(),
                course.getCompletionRule(),
                current.progressPercent(),
                current.totalLessons(),
                current.completedLessons(),
                now));
    }

    private LessonProgress getOrCreateProgress(long studentId, Lesson lesson, Instant now) {
        long courseId = lesson.getSection().getCourse().getId();
        return progress.findByStudentIdAndCourseIdAndLessonId(studentId, courseId, lesson.getId())
                .orElseGet(() -> new LessonProgress(studentId, courseId, lesson.getId(), now));
    }

    private static HeartbeatResult heartbeatResult(Lesson lesson, LessonProgress progress, boolean duplicate) {
        return new HeartbeatResult(
                lesson.getId(),
                progress.getWatchedSeconds(),
                progress.getUpdatedAt(),
                progress.getProgressPercent(),
                progress.getStatus(),
                progress.getStatus() == LessonProgressStatus.COMPLETED,
                duplicate);
    }

    public record ProgressView(
            long courseId,
            int totalLessons,
            int completedLessons,
            BigDecimal progressPercent,
            Instant lastAccessedAt,
            Instant completedAt) {
    }

    public record LessonAccessView(String state, Instant releaseAt, LessonPrerequisiteRef prerequisite) {
    }

    public record MyLesson(
            Long id,
            String title,
            LessonType lessonType,
            int position,
            Integer durationSeconds,
            boolean mandatory,
            LessonProgressStatus status,
            Instant completedAt,
            LessonAccessView access) {
    }

    public record MySection(Long id, String title, int position, Instant releaseAt, List<MyLesson> lessons) {
    }

    public record HeartbeatResult(
            Long lessonId,
            int progressSeconds,
            Instant updatedAt,
            BigDecimal progressPercent,
            LessonProgressStatus status,
            boolean completed,
            boolean duplicate) {
    }

    public record CompleteResult(Long lessonId, LessonProgressStatus status, Instant completedAt) {
    }
}
