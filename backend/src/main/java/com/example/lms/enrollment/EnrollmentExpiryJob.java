package com.example.lms.enrollment;

import java.time.Clock;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chuyển Enrollment quá hạn sang EXPIRED để báo cáo gọn (Spec 12.3). Không phải lớp chặn quyền:
 * {@link EnrollmentAccessService} luôn kiểm tra thời gian ở mỗi request.
 */
@Component
public class EnrollmentExpiryJob {

    private final EnrollmentRepository enrollments;
    private final Clock clock;

    public EnrollmentExpiryJob(EnrollmentRepository enrollments, Clock clock) {
        this.enrollments = enrollments;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${enrollment.expiry-job-delay:PT5M}")
    @Transactional
    public void expireDueEnrollments() {
        enrollments.expireDue(EnrollmentStatus.ACTIVE, EnrollmentStatus.EXPIRED, clock.instant());
    }
}
