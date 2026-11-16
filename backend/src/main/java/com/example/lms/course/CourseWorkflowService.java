package com.example.lms.course;

import com.example.lms.audit.AuditLogService;
import com.example.lms.auth.AuthException;
import com.example.lms.auth.UserPrincipal;
import com.example.lms.common.PageMeta;
import com.example.lms.common.PageResponse;
import com.example.lms.file.FileStorageService;
import com.example.lms.user.User;
import com.example.lms.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class CourseWorkflowService {

    private final CourseRepository courseRepository;
    private final CourseReviewRequestRepository reviewRequestRepository;
    private final CourseSectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    public CourseWorkflowService(
            CourseRepository courseRepository,
            CourseReviewRequestRepository reviewRequestRepository,
            CourseSectionRepository sectionRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            AuditLogService auditLogService) {
        this.courseRepository = courseRepository;
        this.reviewRequestRepository = reviewRequestRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ReviewRequestSummary submitReview(
            UserPrincipal principal,
            Long courseId,
            ReviewSubmissionRequest request) {
        Course course = requireTeacherCourse(principal.id(), courseId);
        requireStatus(course, CourseStatus.DRAFT);
        if (sectionRepository.countByCourse_Id(courseId) == 0
                || !sectionRepository.existsByCourse_IdAndPublishedTrue(courseId)
                || !lessonRepository.existsBySection_Course_IdAndPublishedTrueAndMandatoryTrue(courseId)
                || course.getThumbnailFileId() == null) {
            throw reviewRequirementsNotMet();
        }
        fileStorageService.validateCourseThumbnail(course.getThumbnailFileId());

        User teacher = requireUser(principal.id());
        CourseReviewRequest reviewRequest = reviewRequestRepository.saveAndFlush(
                new CourseReviewRequest(course, teacher, trimToNull(request.note())));
        course.submitForReview();
        courseRepository.saveAndFlush(course);
        recordAudit(
                teacher,
                "COURSE_REVIEW_SUBMITTED",
                course,
                CourseStatus.DRAFT,
                CourseStatus.PENDING_REVIEW,
                request.note());
        return ReviewRequestSummary.from(reviewRequest);
    }

    @Transactional
    public ReviewDecisionResult withdrawReview(UserPrincipal principal, Long courseId) {
        Course course = requireTeacherCourse(principal.id(), courseId);
        requireStatus(course, CourseStatus.PENDING_REVIEW);
        CourseReviewRequest reviewRequest = requirePendingReview(courseId);
        course.withdrawReview();
        reviewRequest.complete(CourseReviewStatus.WITHDRAWN, null, null, Instant.now());
        reviewRequestRepository.saveAndFlush(reviewRequest);
        courseRepository.saveAndFlush(course);
        User teacher = requireUser(principal.id());
        recordAudit(
                teacher,
                "COURSE_REVIEW_WITHDRAWN",
                course,
                CourseStatus.PENDING_REVIEW,
                CourseStatus.DRAFT,
                null);
        return ReviewDecisionResult.from(course, null, null);
    }

    @Transactional
    public ReviewDecisionResult reopen(UserPrincipal principal, Long courseId) {
        Course course = requireTeacherCourse(principal.id(), courseId);
        requireStatus(course, CourseStatus.REJECTED);
        course.reopen();
        courseRepository.saveAndFlush(course);
        User teacher = requireUser(principal.id());
        String reason = latestRejectedReview(courseId).map(CourseReviewRequest::getReviewNote).orElse(null);
        recordAudit(teacher, "COURSE_REOPENED", course, CourseStatus.REJECTED, CourseStatus.DRAFT, reason);
        return ReviewDecisionResult.from(course, null, reason);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewRequestSummary> listReviewRequests(
            CourseReviewStatus status,
            int page,
            int size,
            String sortExpression) {
        Page<CourseReviewRequest> requests = reviewRequestRepository.search(
                status,
                PageRequest.of(page, size, parseSort(sortExpression)));
        return new PageResponse<>(
                requests.map(ReviewRequestSummary::from).getContent(),
                new PageMeta(
                        requests.getNumber(),
                        requests.getSize(),
                        requests.getTotalElements(),
                        requests.getTotalPages()));
    }

    @Transactional
    public ReviewDecisionResult approve(
            UserPrincipal principal,
            Long courseId,
            ReviewDecisionRequest request) {
        Course course = requireCourse(courseId);
        requireStatus(course, CourseStatus.PENDING_REVIEW);
        User admin = requireUser(principal.id());
        CourseReviewRequest reviewRequest = requirePendingReview(courseId);
        String reason = request == null ? null : trimToNull(request.reason());
        Instant now = Instant.now();
        reviewRequest.complete(CourseReviewStatus.APPROVED, admin, reason, now);
        course.publish(now);
        reviewRequestRepository.saveAndFlush(reviewRequest);
        courseRepository.saveAndFlush(course);
        recordAudit(admin, "COURSE_APPROVED", course, CourseStatus.PENDING_REVIEW, CourseStatus.PUBLISHED, reason);
        return ReviewDecisionResult.from(course, admin.getId(), reason);
    }

    @Transactional
    public ReviewDecisionResult reject(UserPrincipal principal, Long courseId, RejectCourseRequest request) {
        Course course = requireCourse(courseId);
        requireStatus(course, CourseStatus.PENDING_REVIEW);
        User admin = requireUser(principal.id());
        CourseReviewRequest reviewRequest = requirePendingReview(courseId);
        String reason = request.reason().trim();
        Instant now = Instant.now();
        reviewRequest.complete(CourseReviewStatus.REJECTED, admin, reason, now);
        course.reject();
        reviewRequestRepository.saveAndFlush(reviewRequest);
        courseRepository.saveAndFlush(course);
        recordAudit(admin, "COURSE_REJECTED", course, CourseStatus.PENDING_REVIEW, CourseStatus.REJECTED, reason);
        return ReviewDecisionResult.from(course, admin.getId(), reason);
    }

    @Transactional
    public ReviewDecisionResult hide(UserPrincipal principal, Long courseId, HideCourseRequest request) {
        Course course = requireCourse(courseId);
        requireStatus(course, CourseStatus.PUBLISHED);
        User admin = requireUser(principal.id());
        String reason = request == null ? null : trimToNull(request.reason());
        course.hide(Instant.now());
        courseRepository.saveAndFlush(course);
        recordAudit(admin, "COURSE_HIDDEN", course, CourseStatus.PUBLISHED, CourseStatus.HIDDEN, reason);
        return ReviewDecisionResult.from(course, admin.getId(), reason);
    }

    @Transactional
    public ReviewDecisionResult publishHidden(UserPrincipal principal, Long courseId) {
        Course course = requireCourse(courseId);
        requireStatus(course, CourseStatus.HIDDEN);
        User admin = requireUser(principal.id());
        course.publish(Instant.now());
        courseRepository.saveAndFlush(course);
        recordAudit(admin, "COURSE_PUBLISHED", course, CourseStatus.HIDDEN, CourseStatus.PUBLISHED, null);
        return ReviewDecisionResult.from(course, admin.getId(), null);
    }

    @Transactional
    public ReviewDecisionResult archive(UserPrincipal principal, Long courseId, HideCourseRequest request) {
        Course course = requireCourse(courseId);
        if (course.getStatus() != CourseStatus.PUBLISHED && course.getStatus() != CourseStatus.HIDDEN) {
            throw stateConflict();
        }
        User admin = requireUser(principal.id());
        String reason = request == null ? null : trimToNull(request.reason());
        CourseStatus previousStatus = course.getStatus();
        course.archive(Instant.now());
        courseRepository.saveAndFlush(course);
        recordAudit(admin, "COURSE_ARCHIVED", course, previousStatus, CourseStatus.ARCHIVED, reason);
        return ReviewDecisionResult.from(course, admin.getId(), reason);
    }

    private Course requireTeacherCourse(Long teacherId, Long courseId) {
        return courseRepository.findTeacherCourse(courseId, teacherId)
                .orElseThrow(CourseWorkflowService::courseNotFound);
    }

    private Course requireCourse(Long courseId) {
        return courseRepository.findById(courseId).orElseThrow(CourseWorkflowService::courseNotFound);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        "USER_NOT_FOUND", "Không tìm thấy người dùng.", HttpStatus.NOT_FOUND));
    }

    private CourseReviewRequest requirePendingReview(Long courseId) {
        return reviewRequestRepository
                .findFirstByCourse_IdAndStatusOrderBySubmittedAtDesc(courseId, CourseReviewStatus.PENDING)
                .orElseThrow(CourseWorkflowService::stateConflict);
    }

    private java.util.Optional<CourseReviewRequest> latestRejectedReview(Long courseId) {
        return reviewRequestRepository.findFirstByCourse_IdAndStatusOrderBySubmittedAtDesc(
                courseId, CourseReviewStatus.REJECTED);
    }

    private void recordAudit(
            User actor,
            String action,
            Course course,
            CourseStatus oldStatus,
            CourseStatus newStatus,
            String reason) {
        auditLogService.record(
                actor,
                action,
                "Course",
                course.getId(),
                null,
                null,
                Map.of("status", oldStatus.name()),
                Map.of("status", newStatus.name()),
                reason);
    }

    private static void requireStatus(Course course, CourseStatus expected) {
        if (course.getStatus() != expected) throw stateConflict();
    }

    private static Sort parseSort(String sortExpression) {
        if (sortExpression == null || sortExpression.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "submittedAt").and(Sort.by("id"));
        }
        String[] parts = sortExpression.split(",", 2);
        if (!"submittedAt".equals(parts[0])) {
            throw new AuthException("INVALID_SORT", "Trường sắp xếp không được hỗ trợ.", HttpStatus.BAD_REQUEST);
        }
        Sort.Direction direction = parts.length == 2 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, "submittedAt").and(Sort.by("id"));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static AuthException courseNotFound() {
        return new AuthException(
                "COURSE_NOT_FOUND", "Không tìm thấy khóa học được phân công.", HttpStatus.NOT_FOUND);
    }

    private static AuthException reviewRequirementsNotMet() {
        return new AuthException(
                "COURSE_REVIEW_REQUIREMENTS_NOT_MET",
                "Khóa học cần có chương, bài học bắt buộc đã phát hành và ảnh đại diện trước khi gửi duyệt.",
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private static AuthException stateConflict() {
        return new AuthException(
                "STATE_CONFLICT", "Trạng thái khóa học hiện không cho phép thao tác này.", HttpStatus.CONFLICT);
    }
}
