package com.example.lms.enrollment;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.contract.identity.UserQueryService;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Cổng quyền học dùng chung (điểm giao 4): A gọi khi mở Lesson, C gọi trước khi truy xuất tài liệu AI.
 * Kiểm tra thời gian ở mỗi lần gọi, không tin trạng thái do job cập nhật.
 */
@Service
@Transactional(readOnly = true)
public class EnrollmentAccessService {

    private final EnrollmentRepository enrollments;
    private final UserQueryService users;
    private final Clock clock;

    public EnrollmentAccessService(EnrollmentRepository enrollments, UserQueryService users, Clock clock) {
        this.enrollments = enrollments;
        this.users = users;
        this.clock = clock;
    }

    /**
     * @return Enrollment đang hiệu lực
     * @throws BusinessException {@code ACCOUNT_BLOCKED}, {@code ENROLLMENT_REQUIRED}, {@code ENROLLMENT_EXPIRED}
     *                           hoặc {@code ENROLLMENT_REVOKED}
     */
    public Enrollment requireActiveAccess(long studentId, long courseId) {
        if (!users.isActive(studentId)) {
            throw new BusinessException(ErrorCode.ACCOUNT_BLOCKED, "Account is not active");
        }
        EnrollmentAccessRule.Decision decision = EnrollmentAccessRule.evaluate(
                enrollments.findByStudentIdAndCourseId(studentId, courseId), clock.instant());
        if (!decision.granted()) {
            throw new BusinessException(decision.denial(), "No active enrollment for course " + courseId);
        }
        return decision.enrollment();
    }

    public boolean hasActiveAccess(long studentId, long courseId) {
        return users.isActive(studentId) && EnrollmentAccessRule.evaluate(
                enrollments.findByStudentIdAndCourseId(studentId, courseId), clock.instant()).granted();
    }

    /** Có hoặc từng có Enrollment: C dùng cho quy tắc Teacher xem hồ sơ AI của học viên (Spec 7.4). */
    public boolean hasEverEnrolled(long studentId, long courseId) {
        return enrollments.existsByStudentIdAndCourseId(studentId, courseId);
    }
}
