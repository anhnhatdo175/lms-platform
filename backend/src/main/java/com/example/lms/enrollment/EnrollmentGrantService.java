package com.example.lms.enrollment;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms.commerce.OrderItem;
import com.example.lms.contract.audit.AuditLogService;
import com.example.lms.contract.audit.AuditLogService.AuditEntry;
import com.example.lms.contract.course.CourseQueryService;
import com.example.lms.contract.course.CourseQueryService.CourseSnapshot;
import com.example.lms.contract.identity.UserQueryService;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Cấp và thu hồi quyền học (điểm giao 5). Cấp theo OrderItem chỉ được gọi từ {@code PaymentCompletionService}
 * trong transaction thanh toán; đó là điều kiện duy nhất cấp quyền khi mua hàng (Spec 11.9).
 */
@Service
public class EnrollmentGrantService {

    private final EnrollmentRepository enrollments;
    private final CourseQueryService courses;
    private final UserQueryService users;
    private final AuditLogService audit;
    private final Clock clock;

    public EnrollmentGrantService(EnrollmentRepository enrollments, CourseQueryService courses,
            UserQueryService users, AuditLogService audit, Clock clock) {
        this.enrollments = enrollments;
        this.courses = courses;
        this.users = users;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Idempotent: OrderItem đã có Enrollment thì trả lại bản cũ ({@code UNIQUE(order_item_id)} là chốt chặn cuối).
     * Nếu User đã có Enrollment đang hiệu lực cho Course (ví dụ Admin cấp trong lúc đơn chờ thanh toán) thì không
     * tạo bản ACTIVE thứ hai; ghi AuditLog để Admin xử lý hoàn tiền (Doc phần B, Mục 13 ý 3).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Enrollment> grantForOrderItem(OrderItem item, long studentId, Instant paidAt) {
        Optional<Enrollment> existing = enrollments.findByOrderItemId(item.getId());
        if (existing.isPresent()) {
            return existing;
        }
        List<Enrollment> history = enrollments.lockByStudentAndCourse(studentId, item.getCourseId());
        Optional<Enrollment> active = history.stream().filter(e -> e.isEffectiveAt(paidAt)).findFirst();
        if (active.isPresent()) {
            audit.record(new AuditEntry(null, "ENROLLMENT_GRANT_SKIPPED_ALREADY_ACTIVE", "OrderItem", item.getId(),
                    null, null, "Student already had enrollment " + active.get().getId() + " for course "
                            + item.getCourseId() + "; refund manually"));
            return Optional.empty();
        }
        boolean renewal = closeLapsed(history, paidAt);
        Instant expiresAt = item.getAccessDurationDays() == null ? null
                : paidAt.plus(Duration.ofDays(item.getAccessDurationDays()));
        return Optional.of(enrollments.save(Enrollment.grant(studentId, item.getCourseId(), item.getId(),
                renewal ? EnrollmentSource.RENEWAL : EnrollmentSource.PURCHASE, paidAt, expiresAt, paidAt)));
    }

    /** Admin cấp quyền thủ công; {@code expiresAt} trống thì dùng {@code access_duration_days} của Course. */
    @Transactional
    public Enrollment grantByAdmin(long adminId, long studentId, long courseId, Instant requestedExpiresAt,
            String reason) {
        Instant now = clock.instant();
        if (users.findById(studentId).filter(UserQueryService.UserSummary::active).isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Student " + studentId + " is not an active user");
        }
        CourseSnapshot course = courses.findCourse(courseId).orElseThrow(
                () -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Course " + courseId + " does not exist"));
        if (requestedExpiresAt != null && !requestedExpiresAt.isAfter(now)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "expiresAt must be in the future");
        }
        List<Enrollment> history = enrollments.lockByStudentAndCourse(studentId, courseId);
        if (history.stream().anyMatch(e -> e.isEffectiveAt(now))) {
            throw new BusinessException(ErrorCode.COURSE_ALREADY_ENROLLED,
                    "Student already has an active enrollment for course " + courseId);
        }
        closeLapsed(history, now);
        Instant expiresAt = requestedExpiresAt != null ? requestedExpiresAt
                : course.accessDurationDays() == null ? null
                        : now.plus(Duration.ofDays(course.accessDurationDays()));
        Enrollment granted = enrollments.save(Enrollment.grant(studentId, courseId, null,
                EnrollmentSource.ADMIN_GRANTED, now, expiresAt, now));
        audit.record(new AuditEntry(adminId, "ENROLLMENT_GRANTED", "Enrollment", granted.getId(), null,
                "{\"studentId\":" + studentId + ",\"courseId\":" + courseId + "}", reason));
        return granted;
    }

    @Transactional
    public Enrollment revoke(long adminId, long enrollmentId, String reason) {
        Enrollment enrollment = enrollments.findById(enrollmentId)
                .orElseThrow(() -> BusinessException.notFound("Enrollment"));
        Instant now = clock.instant();
        enrollment.revoke(adminId, reason, now);
        audit.record(new AuditEntry(adminId, "ENROLLMENT_REVOKED", "Enrollment", enrollmentId,
                "{\"status\":\"ACTIVE\"}", "{\"status\":\"REVOKED\"}", reason));
        return enrollment;
    }

    /** Chuyển các bản ACTIVE đã quá hạn sang EXPIRED; trả {@code true} nếu User từng có Enrollment hết hạn. */
    private static boolean closeLapsed(List<Enrollment> history, Instant now) {
        history.stream().filter(e -> e.isLapsedAt(now)).forEach(e -> e.expire(now));
        return history.stream().anyMatch(e -> e.getStatus() == EnrollmentStatus.EXPIRED);
    }
}
