package com.example.lms.enrollment;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.enrollment.dto.AdminEnrollmentResponse;
import com.example.lms.enrollment.dto.EnrollmentSummaryResponse;
import com.example.lms.enrollment.dto.GrantEnrollmentRequest;
import com.example.lms.enrollment.dto.MyCourseDetailResponse;
import com.example.lms.enrollment.dto.RevokeEnrollmentRequest;
import com.example.lms.shared.pagination.PageRequests;
import com.example.lms.shared.pagination.PageResponse;
import com.example.lms.shared.web.ApiResponse;

import jakarta.validation.Valid;

/** OpenAPI tag {@code my-courses} và {@code /admin/enrollments*}. */
@RestController
public class EnrollmentController {

    private final MyCoursesService myCourses;
    private final AdminEnrollmentService admin;
    private final CurrentUserProvider currentUser;

    public EnrollmentController(MyCoursesService myCourses, AdminEnrollmentService admin,
            CurrentUserProvider currentUser) {
        this.myCourses = myCourses;
        this.admin = admin;
        this.currentUser = currentUser;
    }

    @GetMapping("/my-courses")
    public PageResponse<EnrollmentSummaryResponse> myCourses(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequests.of(page, size, Sort.unsorted());
        return myCourses.list(currentUser.current().id(), page, size);
    }

    @GetMapping("/my-courses/{courseId}")
    public ApiResponse<MyCourseDetailResponse> myCourse(@PathVariable long courseId) {
        return ApiResponse.of(myCourses.get(currentUser.current().id(), courseId));
    }

    @GetMapping("/admin/enrollments")
    public PageResponse<AdminEnrollmentResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long studentId, @RequestParam(required = false) EnrollmentStatus status) {
        return PageResponse.of(admin.list(currentUser.current(), courseId, studentId, status,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), dto -> dto);
    }

    @PostMapping("/admin/enrollments")
    public ResponseEntity<ApiResponse<AdminEnrollmentResponse>> grant(
            @Valid @RequestBody GrantEnrollmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(admin.grant(currentUser.current(), request)));
    }

    @PostMapping("/admin/enrollments/{enrollmentId}/revoke")
    public ApiResponse<AdminEnrollmentResponse> revoke(@PathVariable long enrollmentId,
            @Valid @RequestBody RevokeEnrollmentRequest request) {
        return ApiResponse.of(admin.revoke(currentUser.current(), enrollmentId, request.reason()));
    }
}
