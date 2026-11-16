package com.example.lms.course;

import com.example.lms.audit.AuditLogService;
import com.example.lms.auth.AuthException;
import com.example.lms.auth.UserPrincipal;
import com.example.lms.file.FileStorageService;
import com.example.lms.user.User;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseWorkflowServiceTest {

    @Test
    void submitReviewRequiresPublishedMandatoryCurriculumAndRecordsAudit() {
        CourseRepository courses = mock(CourseRepository.class);
        CourseReviewRequestRepository reviews = mock(CourseReviewRequestRepository.class);
        CourseSectionRepository sections = mock(CourseSectionRepository.class);
        LessonRepository lessons = mock(LessonRepository.class);
        UserRepository users = mock(UserRepository.class);
        FileStorageService files = mock(FileStorageService.class);
        AuditLogService audit = mock(AuditLogService.class);
        User teacher = new User("Teacher", "teacher@example.com", "hash", UserStatus.ACTIVE);
        Course course = new Course(
                new CourseCategory("General", "general", null),
                teacher,
                "Course",
                "course",
                null,
                null,
                1000,
                500,
                12L,
                null,
                null,
                null,
                CourseCompletionRule.ALL_MANDATORY_LESSONS);
        when(courses.findTeacherCourse(1L, 1L)).thenReturn(Optional.of(course));
        when(courses.saveAndFlush(any(Course.class))).thenAnswer(call -> call.getArgument(0));
        when(reviews.saveAndFlush(any(CourseReviewRequest.class))).thenAnswer(call -> call.getArgument(0));
        when(sections.countByCourse_Id(1L)).thenReturn(1L);
        when(sections.existsByCourse_IdAndPublishedTrue(1L)).thenReturn(true);
        when(lessons.existsBySection_Course_IdAndPublishedTrueAndMandatoryTrue(1L)).thenReturn(true);
        when(users.findById(1L)).thenReturn(Optional.of(teacher));

        CourseWorkflowService service = new CourseWorkflowService(
                courses, reviews, sections, lessons, users, files, audit);
        ReviewRequestSummary result = service.submitReview(
                new UserPrincipal(1L, "Teacher", "teacher@example.com", Set.of()),
                1L,
                new ReviewSubmissionRequest("Ready for review"));

        assertThat(course.getStatus()).isEqualTo(CourseStatus.PENDING_REVIEW);
        assertThat(result.status()).isEqualTo(CourseReviewStatus.PENDING);
        assertThat(result.courseTitle()).isEqualTo("Course");
        verify(files).validateCourseThumbnail(12L);
        verify(audit).record(
                teacher,
                "COURSE_REVIEW_SUBMITTED",
                "Course",
                null,
                null,
                null,
                java.util.Map.of("status", "DRAFT"),
                java.util.Map.of("status", "PENDING_REVIEW"),
                "Ready for review");
    }

    @Test
    void reopenCannotBeUsedBeforeCourseIsRejected() {
        CourseRepository courses = mock(CourseRepository.class);
        Course course = new Course(
                new CourseCategory("General", "general", null),
                new User("Teacher", "teacher@example.com", "hash", UserStatus.ACTIVE),
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
        when(courses.findTeacherCourse(1L, 1L)).thenReturn(Optional.of(course));
        CourseWorkflowService service = new CourseWorkflowService(
                courses,
                mock(CourseReviewRequestRepository.class),
                mock(CourseSectionRepository.class),
                mock(LessonRepository.class),
                mock(UserRepository.class),
                mock(FileStorageService.class),
                mock(AuditLogService.class));

        assertThatThrownBy(() -> service.reopen(
                        new UserPrincipal(1L, "Teacher", "teacher@example.com", Set.of()), 1L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Trạng thái khóa học");
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
    }
}
