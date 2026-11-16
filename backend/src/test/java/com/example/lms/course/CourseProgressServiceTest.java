package com.example.lms.course;

import com.example.lms.enrollment.Enrollment;
import com.example.lms.enrollment.EnrollmentRepository;
import com.example.lms.exam.ExamRepository;
import com.example.lms.exam.ExamResultQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseProgressServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T09:00:00Z");

    @Test
    void lessonRemainsLockedUntilItsSectionReleaseTime() {
        Lesson testLesson = lesson(CourseStatus.PUBLISHED, 3);
        Enrollment enrollment = mock(Enrollment.class);
        when(enrollment.getStartsAt()).thenReturn(NOW.minusSeconds(86_400));
        EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
        when(enrollments.findByStudentIdAndCourseId(7L, 10L)).thenReturn(List.of(enrollment));
        LessonRepository lessons = mock(LessonRepository.class);
        when(lessons.findById(testLesson.getId())).thenReturn(Optional.of(testLesson));
        CourseProgressService service = service(
                enrollments, mock(LessonPrerequisiteRepository.class), mock(LessonProgressRepository.class), lessons);

        var access = service.checkOpen(7L, testLesson.getId());

        assertThat(access.open()).isFalse();
        assertThat(access.lockReason()).isEqualTo(
                com.example.lms.shared.exception.ErrorCode.LESSON_LOCKED_UNTIL_RELEASE);
        assertThat(access.releaseAt()).isEqualTo(NOW.plusSeconds(2 * 86_400));
    }

    @Test
    void lessonRemainsLockedUntilRequiredLessonIsCompleted() {
        Lesson testLesson = lesson(CourseStatus.PUBLISHED, 0);
        Lesson prerequisiteLesson = lesson(CourseStatus.PUBLISHED, 0);
        ReflectionTestUtils.setField(prerequisiteLesson, "id", 302L);
        LessonPrerequisite prerequisite = new LessonPrerequisite(
                testLesson,
                prerequisiteLesson,
                null,
                PrerequisiteConditionType.COMPLETE_LESSON);
        LessonPrerequisiteRepository prerequisites = mock(LessonPrerequisiteRepository.class);
        when(prerequisites.findByLesson_Id(testLesson.getId())).thenReturn(Optional.of(prerequisite));
        LessonProgressRepository progress = mock(LessonProgressRepository.class);
        when(progress.existsByStudentIdAndLessonIdAndStatus(
                        7L, 302L, LessonProgressStatus.COMPLETED))
                .thenReturn(false);
        LessonRepository lessons = mock(LessonRepository.class);
        when(lessons.findById(testLesson.getId())).thenReturn(Optional.of(testLesson));
        CourseProgressService service = service(
                mock(EnrollmentRepository.class), prerequisites, progress, lessons);

        var access = service.checkOpen(7L, testLesson.getId());

        assertThat(access.open()).isFalse();
        assertThat(access.lockReason()).isEqualTo(
                com.example.lms.shared.exception.ErrorCode.LESSON_LOCKED_BY_PREREQUISITE);
    }

    @Test
    void heartbeatKeepsTheFurthestPositionAndRecognizesTheLatestEventAsDuplicate() {
        LessonProgress progress = new LessonProgress(7L, 10L, 301L, NOW);
        progress.recordHeartbeat("event-1", 40, 100, NOW);
        progress.recordHeartbeat("event-2", 25, 100, NOW.plusSeconds(5));

        assertThat(progress.getWatchedSeconds()).isEqualTo(40);
        assertThat(progress.getProgressPercent()).isEqualByComparingTo("40.00");
        assertThat(progress.isDuplicateHeartbeat("event-2")).isTrue();
    }

    private static CourseProgressService service(
            EnrollmentRepository enrollments,
            LessonPrerequisiteRepository prerequisites,
            LessonProgressRepository progress,
            LessonRepository lessons) {
        return new CourseProgressService(
                mock(CourseRepository.class),
                mock(CourseSectionRepository.class),
                lessons,
                prerequisites,
                progress,
                mock(CourseCompletionRepository.class),
                enrollments,
                mock(ExamRepository.class),
                mock(ExamResultQueryService.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Lesson lesson(CourseStatus status, int releaseAfterDays) {
        Course course = new Course(
                new CourseCategory("General", "general", null),
                new com.example.lms.user.User(
                        "Teacher", "teacher@example.com", "hash", com.example.lms.user.UserStatus.ACTIVE),
                "Course",
                "course",
                null,
                null,
                1000,
                500,
                null,
                null,
                null,
                null,
                CourseCompletionRule.ALL_MANDATORY_LESSONS);
        if (status == CourseStatus.PUBLISHED) course.publish(NOW.minusSeconds(10 * 86_400));
        ReflectionTestUtils.setField(course, "id", 10L);
        CourseSection section = new CourseSection(course, "Section", null, 1, true, releaseAfterDays);
        Lesson lesson = new Lesson(
                section,
                "Lesson",
                null,
                null,
                LessonType.TEXT,
                null,
                null,
                1,
                false,
                true,
                true,
                CompletionMethod.MANUAL,
                null);
        ReflectionTestUtils.setField(lesson, "id", 301L);
        return lesson;
    }
}
