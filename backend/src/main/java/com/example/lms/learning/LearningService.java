package com.example.lms.learning;

import com.example.lms.auth.AuthException;
import com.example.lms.contract.course.LessonAccessService.LessonAccess;
import com.example.lms.course.CourseProgressService;
import com.example.lms.course.CourseProgressService.CompleteResult;
import com.example.lms.course.CourseProgressService.HeartbeatResult;
import com.example.lms.course.CourseProgressService.ProgressView;
import com.example.lms.course.Lesson;
import com.example.lms.course.LessonPrerequisite;
import com.example.lms.course.LessonPrerequisiteRef;
import com.example.lms.course.LessonPrerequisiteRepository;
import com.example.lms.course.LessonProgress;
import com.example.lms.course.LessonResourceRepository;
import com.example.lms.course.LessonResourceSummary;
import com.example.lms.course.LessonRepository;
import com.example.lms.enrollment.EnrollmentAccessService;
import com.example.lms.exam.ExamRepository;
import com.example.lms.file.FileStorageService;
import com.example.lms.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LearningService {

    private final LessonRepository lessons;
    private final LessonResourceRepository lessonResources;
    private final LessonPrerequisiteRepository prerequisites;
    private final ExamRepository exams;
    private final EnrollmentAccessService enrollmentAccess;
    private final CourseProgressService progress;
    private final FileStorageService files;

    public LearningService(
            LessonRepository lessons,
            LessonResourceRepository lessonResources,
            LessonPrerequisiteRepository prerequisites,
            ExamRepository exams,
            EnrollmentAccessService enrollmentAccess,
            CourseProgressService progress,
            FileStorageService files) {
        this.lessons = lessons;
        this.lessonResources = lessonResources;
        this.prerequisites = prerequisites;
        this.exams = exams;
        this.enrollmentAccess = enrollmentAccess;
        this.progress = progress;
        this.files = files;
    }

    @Transactional
    public LessonLearningView getLesson(long studentId, long lessonId) {
        Lesson lesson = requireAccessibleLesson(studentId, lessonId);
        LessonProgress lessonProgress = progress.markLessonOpened(studentId, lesson);
        FileStorageService.TemporaryResourceUrl video = files.lessonVideoUrl(lesson.getVideoResourceId());
        return new LessonLearningView(
                lesson.getId(),
                lesson.getSection().getCourse().getId(),
                lesson.getSection().getId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getContent(),
                lesson.getLessonType(),
                lesson.getPosition(),
                video.url(),
                video.expiresAt(),
                lesson.getDurationSeconds(),
                lesson.isMandatory(),
                lesson.getCompletionMethod(),
                lesson.getCompletionThreshold(),
                exams.findByLessonId(lessonId).map(exam -> exam.getId()).orElse(null),
                lessonResources.findAllByLesson_IdOrderByPositionAscIdAsc(lessonId).stream()
                        .map(resource -> LessonResourceSummary.from(
                                resource,
                                resource.isDownloadable()
                                        ? files.lessonResourceFile(resource.getResourceId()).url()
                                        : null))
                        .toList(),
                new LessonLearningView.Progress(
                        lessonProgress.getStatus(),
                        lessonProgress.getWatchedSeconds(),
                        lessonProgress.getProgressPercent(),
                        lessonProgress.getCompletedAt()));
    }

    @Transactional
    public HeartbeatResult heartbeat(long studentId, long lessonId, LessonHeartbeatRequest request) {
        Lesson lesson = requireAccessibleLesson(studentId, lessonId);
        return progress.heartbeat(
                studentId,
                lesson,
                request.positionSeconds(),
                request.clientEventId().toString());
    }

    @Transactional
    public CompleteResult complete(long studentId, long lessonId, CompleteLessonRequest request) {
        Lesson lesson = requireAccessibleLesson(studentId, lessonId);
        return progress.completeManually(
                studentId,
                lesson,
                request.clientEventId() == null ? null : request.clientEventId().toString());
    }

    public ProgressView courseProgress(long studentId, long courseId) {
        enrollmentAccess.requireActiveAccess(studentId, courseId);
        return progress.courseProgress(studentId, courseId);
    }

    private Lesson requireAccessibleLesson(long studentId, long lessonId) {
        Lesson lesson = lessons.findById(lessonId)
                .orElseThrow(() -> new AuthException(
                        "LESSON_NOT_FOUND", "Không tìm thấy bài học.", HttpStatus.NOT_FOUND));
        long courseId = lesson.getSection().getCourse().getId();
        enrollmentAccess.requireActiveAccess(studentId, courseId);
        LessonAccess access = progress.checkOpen(studentId, lessonId);
        if (!access.open()) {
            LessonPrerequisiteRef prerequisite = prerequisites.findByLesson_Id(lessonId)
                    .map(LessonPrerequisite::toRef)
                    .orElse(null);
            ErrorCode code = access.lockReason() == null ? ErrorCode.FORBIDDEN : access.lockReason();
            throw new LessonLockedException(code, access.releaseAt(), prerequisite);
        }
        return lesson;
    }
}
