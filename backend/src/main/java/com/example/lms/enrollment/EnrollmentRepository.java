package com.example.lms.enrollment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentIdAndCourseId(long studentId, long courseId);

    boolean existsByStudentIdAndCourseId(long studentId, long courseId);

    List<Enrollment> findByStudentIdOrderByCreatedAtDescIdDesc(long studentId);

    @Query("""
            select e from Enrollment e
            where (:courseId is null or e.courseId = :courseId)
              and (:studentId is null or e.studentId = :studentId)
              and (:status is null or e.status = :status)
            """)
    Page<Enrollment> searchForAdmin(@Param("courseId") Long courseId, @Param("studentId") Long studentId,
            @Param("status") EnrollmentStatus status, Pageable pageable);

    Optional<Enrollment> findByOrderItemId(long orderItemId);

    /**
     * Khóa mọi Enrollment của (student, course) trước khi cấp mới, rồi kiểm tra lại sau khi có khóa. Ở
     * REPEATABLE READ, InnoDB khóa cả khoảng trống trên index {@code (student_id, course_id, status)}, nên chặn
     * được insert song song cho cùng cặp dù chưa có dòng nào (ERD Mục 15, ý 1).
     *
     * <p>Đã đo trên MySQL 8.0: khóa khoảng trống chặn luôn insert của học viên khác rơi vào cùng khoảng index,
     * và hai transaction cùng khóa có thể deadlock (bên bị hủy phải thử lại). Phương án cột sinh
     * {@code active_pair_key} + UNIQUE chỉ chặn đúng cặp; chờ nhóm chốt (Doc thiết kế phần B, Mục 8).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enrollment e where e.studentId = :studentId and e.courseId = :courseId")
    List<Enrollment> lockByStudentAndCourse(@Param("studentId") long studentId, @Param("courseId") long courseId);

    /** Job hết hạn chỉ cập nhật trạng thái cho báo cáo; quyền học luôn kiểm tra thời gian ở mỗi request. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Enrollment e set e.status = :expired, e.updatedAt = :now
            where e.status = :active and e.expiresAt is not null and e.expiresAt <= :now
            """)
    int expireDue(@Param("active") EnrollmentStatus active, @Param("expired") EnrollmentStatus expired,
            @Param("now") Instant now);
}
