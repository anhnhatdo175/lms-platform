package com.example.lms.course;

import com.example.lms.audit.AuditLogService;
import com.example.lms.auth.AuthException;
import com.example.lms.auth.UserPrincipal;
import com.example.lms.user.User;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserRole;
import com.example.lms.user.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CourseAdminServiceTest {

    @Test
    void assigningTeachersReplacesOldAssignmentsAndRecordsAudit() {
        CourseRepository courses = mock(CourseRepository.class);
        CourseTeacherRepository assignments = mock(CourseTeacherRepository.class);
        UserRepository users = mock(UserRepository.class);
        CourseService courseService = mock(CourseService.class);
        AuditLogService audit = mock(AuditLogService.class);
        Course course = mock(Course.class);
        User actor = mock(User.class);
        User previousTeacher = teacher(2L);
        User nextTeacher = teacher(3L);
        CourseTeacher previous = new CourseTeacher(course, previousTeacher);
        AtomicReference<List<CourseTeacher>> savedAssignments = new AtomicReference<>(List.of());

        when(course.getId()).thenReturn(10L);
        when(courses.findById(10L)).thenReturn(Optional.of(course));
        when(users.findAllById(List.of(3L))).thenReturn(List.of(nextTeacher));
        when(users.findById(1L)).thenReturn(Optional.of(actor));
        when(assignments.findAllByIdCourseIdAndActiveTrueOrderByPrimaryDescIdTeacherIdAsc(10L))
                .thenReturn(List.of(previous))
                .thenAnswer(invocation -> savedAssignments.get().stream()
                        .filter(CourseTeacher::isActive)
                        .toList());
        when(assignments.findAllByIdCourseIdOrderByIdTeacherIdAsc(10L)).thenReturn(List.of(previous));
        when(assignments.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            savedAssignments.set(invocation.getArgument(0));
            return savedAssignments.get();
        });

        CourseAdminService service = new CourseAdminService(courses, assignments, users, courseService, audit);
        List<CourseTeacherSummary> result = service.assignTeachers(
                new UserPrincipal(1L, "Admin", "admin@example.com", Set.of(UserRole.ADMIN)),
                10L,
                new AssignTeachersRequest(List.of(new TeacherAssignmentRequest(3L, true))));

        assertThat(previous.isActive()).isFalse();
        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.teacherId()).isEqualTo(3L);
            assertThat(summary.primary()).isTrue();
            assertThat(summary.active()).isTrue();
        });
        verify(assignments).saveAllAndFlush(anyList());
        verify(audit).record(
                actor,
                "COURSE_TEACHERS_ASSIGNED",
                "Course",
                10L,
                null,
                null,
                java.util.Map.of("teacherIds", List.of(2L)),
                java.util.Map.of("teacherIds", List.of(3L)),
                null);
    }

    @Test
    void assigningRejectsDuplicateTeacherIdsBeforeLoadingUsers() {
        CourseRepository courses = mock(CourseRepository.class);
        CourseTeacherRepository assignments = mock(CourseTeacherRepository.class);
        UserRepository users = mock(UserRepository.class);
        Course course = mock(Course.class);
        when(courses.findById(10L)).thenReturn(Optional.of(course));

        CourseAdminService service = new CourseAdminService(
                courses,
                assignments,
                users,
                mock(CourseService.class),
                mock(AuditLogService.class));

        assertThatThrownBy(() -> service.assignTeachers(
                        new UserPrincipal(1L, "Admin", "admin@example.com", Set.of(UserRole.ADMIN)),
                        10L,
                        new AssignTeachersRequest(List.of(
                                new TeacherAssignmentRequest(3L, true),
                                new TeacherAssignmentRequest(3L, false)))))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Danh sách giảng viên");

        verifyNoInteractions(users, assignments);
    }

    private static User teacher(Long id) {
        User teacher = mock(User.class);
        when(teacher.getId()).thenReturn(id);
        when(teacher.getFullName()).thenReturn("Teacher " + id);
        when(teacher.getEmail()).thenReturn("teacher" + id + "@example.com");
        when(teacher.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(teacher.hasRole(UserRole.TEACHER)).thenReturn(true);
        return teacher;
    }
}
