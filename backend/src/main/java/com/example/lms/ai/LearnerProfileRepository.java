package com.example.lms.ai;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;

public interface LearnerProfileRepository extends JpaRepository<LearnerProfile, Long> {

    Optional<LearnerProfile> findByStudentId(long studentId);

    /** Tạo hồ sơ rỗng nếu chưa có; an toàn khi nhiều luồng cùng tạo (UNIQUE student_id). */
    @Transactional
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO learner_profiles
                (student_id, profile_vector_version, profile_version, created_at, updated_at)
            VALUES (:studentId, 1, 1, :now, :now)
            """, nativeQuery = true)
    int insertIfAbsent(long studentId, Instant now);

    /**
     * Khoá dòng hồ sơ ({@code SELECT ... FOR UPDATE}) để tuần tự hoá mọi cập nhật hồ sơ của MỘT học viên: chấm bài thi
     * (B) và nộp bài luyện tập có thể chạy đồng thời và nếu không khoá sẽ mất cập nhật EMA.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM LearnerProfile p WHERE p.studentId = :studentId")
    Optional<LearnerProfile> lockByStudentId(long studentId);
}
