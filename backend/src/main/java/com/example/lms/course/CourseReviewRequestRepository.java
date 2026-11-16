package com.example.lms.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseReviewRequestRepository extends JpaRepository<CourseReviewRequest, Long> {

    Optional<CourseReviewRequest> findFirstByCourse_IdAndStatusOrderBySubmittedAtDesc(
            Long courseId,
            CourseReviewStatus status);

    @Query("""
            select request from CourseReviewRequest request
            where (:status is null or request.status = :status)
            """)
    Page<CourseReviewRequest> search(
            @Param("status") CourseReviewStatus status,
            Pageable pageable);
}
