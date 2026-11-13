package com.example.lms.enrollment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.contract.course.CourseProgressQuery;
import com.example.lms.contract.course.CourseProgressQuery.MyCourseOutline;
import com.example.lms.contract.course.CourseQueryService;
import com.example.lms.contract.course.CourseQueryService.CourseSnapshot;
import com.example.lms.enrollment.dto.EnrollmentSummaryResponse;
import com.example.lms.enrollment.dto.MyCourseDetailResponse;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.pagination.PageResponse;

/** {@code GET /my-courses*}: Enrollment của B ghép với tiến độ và curriculum của A (interface M2). */
@Service
@Transactional(readOnly = true)
public class MyCoursesService {

    private final EnrollmentRepository enrollments;
    private final EnrollmentAccessService access;
    private final CourseQueryService courses;
    private final CourseProgressQuery progress;
    private final Clock clock;

    public MyCoursesService(EnrollmentRepository enrollments, EnrollmentAccessService access,
            CourseQueryService courses, CourseProgressQuery progress, Clock clock) {
        this.enrollments = enrollments;
        this.access = access;
        this.courses = courses;
        this.progress = progress;
        this.clock = clock;
    }

    /** Mỗi Course một dòng: Enrollment mới nhất, kể cả đã hết hạn, để giao diện hiện nút "Mua lại". */
    public PageResponse<EnrollmentSummaryResponse> list(long studentId, int page, int size) {
        Map<Long, Enrollment> latestByCourse = new LinkedHashMap<>();
        enrollments.findByStudentIdOrderByCreatedAtDescIdDesc(studentId)
                .forEach(e -> latestByCourse.putIfAbsent(e.getCourseId(), e));
        List<Enrollment> pageItems = latestByCourse.values().stream().skip((long) page * size).limit(size).toList();
        List<Long> courseIds = pageItems.stream().map(Enrollment::getCourseId).toList();
        Map<Long, CourseSnapshot> snapshots = courses.findCourses(courseIds).stream()
                .collect(Collectors.toMap(CourseSnapshot::id, Function.identity()));
        Map<Long, BigDecimal> percents = courseIds.isEmpty() ? Map.of()
                : progress.progressPercents(studentId, courseIds);
        Instant now = clock.instant();
        List<EnrollmentSummaryResponse> data = pageItems.stream()
                .map(e -> summary(e, snapshots.get(e.getCourseId()), percents.get(e.getCourseId()), now)).toList();
        return PageResponse.of(data, page, size, latestByCourse.size());
    }

    /** Chưa từng ghi danh: 404; đã hết hạn hoặc bị thu hồi: 403 kèm mã ENROLLMENT_*. */
    public MyCourseDetailResponse get(long studentId, long courseId) {
        if (!enrollments.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw BusinessException.notFound("Enrollment");
        }
        Enrollment enrollment = access.requireActiveAccess(studentId, courseId);
        CourseSnapshot course = courses.findCourse(courseId).orElse(null);
        MyCourseOutline outline = progress.outline(studentId, courseId);
        BigDecimal percent = progress.progressPercents(studentId, List.of(courseId)).get(courseId);
        return new MyCourseDetailResponse(summary(enrollment, course, percent, clock.instant()), outline.progress(),
                outline.sections());
    }

    private static EnrollmentSummaryResponse summary(Enrollment e, CourseSnapshot course, BigDecimal percent,
            Instant now) {
        EnrollmentStatus status = e.isLapsedAt(now) ? EnrollmentStatus.EXPIRED : e.getStatus();
        return new EnrollmentSummaryResponse(e.getId(), e.getCourseId(), course == null ? null : course.slug(),
                course == null ? null : course.title(), status, e.getStartsAt(),
                percent == null ? BigDecimal.ZERO : percent, e.getExpiresAt(), e.getSource());
    }
}
