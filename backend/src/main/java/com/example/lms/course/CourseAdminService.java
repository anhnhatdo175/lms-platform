package com.example.lms.course;

import com.example.lms.audit.AuditLogService;
import com.example.lms.auth.AuthException;
import com.example.lms.auth.UserPrincipal;
import com.example.lms.common.PageMeta;
import com.example.lms.common.PageResponse;
import com.example.lms.user.User;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserRole;
import com.example.lms.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CourseAdminService {

    private final CourseRepository courseRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final AuditLogService auditLogService;

    public CourseAdminService(
            CourseRepository courseRepository,
            CourseTeacherRepository courseTeacherRepository,
            UserRepository userRepository,
            CourseService courseService,
            AuditLogService auditLogService) {
        this.courseRepository = courseRepository;
        this.courseTeacherRepository = courseTeacherRepository;
        this.userRepository = userRepository;
        this.courseService = courseService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseManaged> list(
            CourseStatus status,
            Long categoryId,
            String query,
            int page,
            int size) {
        Page<Course> courses = courseRepository.searchAdminCourses(
                status,
                categoryId,
                blankToNull(query),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))));
        return new PageResponse<>(
                courses.map(courseService::summarize).getContent(),
                new PageMeta(courses.getNumber(), courses.getSize(), courses.getTotalElements(),
                        courses.getTotalPages()));
    }

    @Transactional
    public List<CourseTeacherSummary> assignTeachers(
            UserPrincipal principal,
            Long courseId,
            AssignTeachersRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AuthException(
                        "COURSE_NOT_FOUND", "Không tìm thấy khóa học.", HttpStatus.NOT_FOUND));
        validateAssignments(request.teachers());

        List<Long> teacherIds = request.teachers().stream()
                .map(TeacherAssignmentRequest::teacherId)
                .toList();
        Map<Long, User> teachers = new HashMap<>();
        userRepository.findAllById(teacherIds).forEach(teacher -> teachers.put(teacher.getId(), teacher));
        if (teachers.size() != teacherIds.size()
                || teachers.values().stream().anyMatch(teacher ->
                        teacher.getStatus() != UserStatus.ACTIVE || !teacher.hasRole(UserRole.TEACHER))) {
            throw invalidTeacherAssignment();
        }

        User actor = requireUser(principal.id());
        List<CourseTeacher> previous = courseTeacherRepository
                .findAllByIdCourseIdAndActiveTrueOrderByPrimaryDescIdTeacherIdAsc(courseId);
        Map<String, Object> oldValue = Map.of(
                "teacherIds", previous.stream().map(assignment -> assignment.getTeacher().getId()).toList());
        Map<Long, CourseTeacher> existing = new HashMap<>();
        courseTeacherRepository.findAllByIdCourseIdOrderByIdTeacherIdAsc(courseId)
                .forEach(assignment -> existing.put(assignment.getTeacher().getId(), assignment));

        Set<Long> requestedIds = new HashSet<>(teacherIds);
        List<CourseTeacher> updates = new ArrayList<>();
        for (CourseTeacher assignment : existing.values()) {
            if (assignment.isActive() && !requestedIds.contains(assignment.getTeacher().getId())) {
                assignment.remove();
                updates.add(assignment);
            }
        }
        for (TeacherAssignmentRequest requested : request.teachers()) {
            CourseTeacher assignment = existing.get(requested.teacherId());
            if (assignment == null) {
                assignment = new CourseTeacher(course, teachers.get(requested.teacherId()));
            }
            assignment.assign(requested.primary(), actor);
            updates.add(assignment);
        }
        courseTeacherRepository.saveAllAndFlush(updates);

        auditLogService.record(
                actor,
                "COURSE_TEACHERS_ASSIGNED",
                "Course",
                courseId,
                null,
                null,
                oldValue,
                Map.of("teacherIds", teacherIds),
                null);
        return courseTeacherRepository.findAllByIdCourseIdAndActiveTrueOrderByPrimaryDescIdTeacherIdAsc(courseId)
                .stream().map(CourseTeacherSummary::from).toList();
    }

    @Transactional
    public void removeTeacher(UserPrincipal principal, Long courseId, Long teacherId) {
        if (!courseRepository.existsById(courseId)) {
            throw new AuthException("COURSE_NOT_FOUND", "Không tìm thấy khóa học.", HttpStatus.NOT_FOUND);
        }
        CourseTeacher assignment = courseTeacherRepository.findById(new CourseTeacherId(courseId, teacherId))
                .filter(CourseTeacher::isActive)
                .orElseThrow(() -> new AuthException(
                        "COURSE_TEACHER_NOT_FOUND", "Không tìm thấy phân công giảng viên.", HttpStatus.NOT_FOUND));
        User actor = requireUser(principal.id());
        assignment.remove();
        courseTeacherRepository.saveAndFlush(assignment);
        auditLogService.record(
                actor,
                "COURSE_TEACHER_REMOVED",
                "Course",
                courseId,
                null,
                null,
                Map.of("teacherId", teacherId),
                Map.of(),
                null);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        "USER_NOT_FOUND", "Không tìm thấy người dùng.", HttpStatus.NOT_FOUND));
    }

    private static void validateAssignments(List<TeacherAssignmentRequest> assignments) {
        long primaryCount = assignments.stream().filter(TeacherAssignmentRequest::primary).count();
        Set<Long> teacherIds = new HashSet<>();
        boolean duplicateTeacher = assignments.stream()
                .map(TeacherAssignmentRequest::teacherId)
                .anyMatch(id -> !teacherIds.add(id));
        if (duplicateTeacher || primaryCount > 1) {
            throw invalidTeacherAssignment();
        }
    }

    private static AuthException invalidTeacherAssignment() {
        return new AuthException(
                "INVALID_TEACHER_ASSIGNMENT",
                "Danh sách giảng viên không hợp lệ.",
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
