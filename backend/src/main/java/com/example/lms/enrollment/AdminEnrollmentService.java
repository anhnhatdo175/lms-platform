package com.example.lms.enrollment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.contract.course.CourseQueryService;
import com.example.lms.contract.course.CourseQueryService.CourseSnapshot;
import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.contract.identity.UserQueryService;
import com.example.lms.enrollment.dto.AdminEnrollmentResponse;
import com.example.lms.enrollment.dto.GrantEnrollmentRequest;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/** {@code /admin/enrollments*}: chỉ Admin; grant và revoke có lý do và AuditLog. */
@Service
public class AdminEnrollmentService {

    private final EnrollmentRepository enrollments;
    private final EnrollmentGrantService grants;
    private final CourseQueryService courses;
    private final UserQueryService users;

    public AdminEnrollmentService(EnrollmentRepository enrollments, EnrollmentGrantService grants,
            CourseQueryService courses, UserQueryService users) {
        this.enrollments = enrollments;
        this.grants = grants;
        this.courses = courses;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<AdminEnrollmentResponse> list(CurrentUser admin, Long courseId, Long studentId,
            EnrollmentStatus status, Pageable pageable) {
        requireAdmin(admin);
        Page<Enrollment> page = enrollments.searchForAdmin(courseId, studentId, status, pageable);
        Map<Long, String> emails = users.emailsByIds(page.getContent().stream().map(Enrollment::getStudentId)
                .distinct().toList());
        Map<Long, String> titles = courses.findCourses(page.getContent().stream().map(Enrollment::getCourseId)
                .distinct().toList()).stream().collect(Collectors.toMap(CourseSnapshot::id, CourseSnapshot::title));
        return page.map(e -> AdminEnrollmentResponse.from(e, emails.get(e.getStudentId()), titles.get(e.getCourseId())));
    }

    @Transactional
    public AdminEnrollmentResponse grant(CurrentUser admin, GrantEnrollmentRequest request) {
        requireAdmin(admin);
        return describe(grants.grantByAdmin(admin.id(), request.studentId(), request.courseId(), request.expiresAt(),
                request.reason()));
    }

    @Transactional
    public AdminEnrollmentResponse revoke(CurrentUser admin, long enrollmentId, String reason) {
        requireAdmin(admin);
        return describe(grants.revoke(admin.id(), enrollmentId, reason));
    }

    private AdminEnrollmentResponse describe(Enrollment e) {
        String email = users.emailsByIds(List.of(e.getStudentId())).get(e.getStudentId());
        String title = courses.findCourse(e.getCourseId()).map(CourseSnapshot::title).orElse(null);
        return AdminEnrollmentResponse.from(e, email, title);
    }

    private static void requireAdmin(CurrentUser user) {
        if (!user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Administrator role required");
        }
    }

}
