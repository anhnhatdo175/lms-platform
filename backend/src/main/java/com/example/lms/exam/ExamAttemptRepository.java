package com.example.lms.exam;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    /** Autosave, nộp bài, chấm, job hết giờ: khóa Attempt để các thao tác trên cùng Attempt chạy tuần tự. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAttempt a where a.id = :id")
    Optional<ExamAttempt> findByIdForUpdate(@Param("id") long id);

    @Query("select a.id from ExamAttempt a where a.examId = :examId and a.studentId = :studentId and a.status = :status")
    Optional<Long> findIdByExamIdAndStudentIdAndStatus(@Param("examId") long examId,
            @Param("studentId") long studentId, @Param("status") AttemptStatus status);

    List<ExamAttempt> findByExamIdAndStudentIdOrderByAttemptNumberAsc(long examId, long studentId);

    List<ExamAttempt> findByExamIdAndStatus(long examId, AttemptStatus status);

    long countByExamIdAndStudentId(long examId, long studentId);

    @Query("select coalesce(max(a.attemptNumber), 0) from ExamAttempt a where a.examId = :examId and a.studentId = :studentId")
    int findMaxAttemptNumber(@Param("examId") long examId, @Param("studentId") long studentId);

    boolean existsByExamId(long examId);

    /** Interface M4 cho A: prerequisite PASS_EXAM và rule completion có bài thi bắt buộc. */
    boolean existsByExamIdAndStudentIdAndStatusAndPassedTrue(long examId, long studentId, AttemptStatus status);

    @Query("""
            select a.id from ExamAttempt a
            where a.status = :inProgress and a.expiresAt <= :now
            order by a.expiresAt asc
            """)
    List<Long> findExpiredInProgressIds(@Param("inProgress") AttemptStatus inProgress, @Param("now") Instant now,
            Pageable pageable);
}
