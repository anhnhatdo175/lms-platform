package com.example.lms.commerce;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(long userId);

    /** Checkout khóa dòng Cart để hai lần checkout của cùng User chạy tuần tự, kể cả khi khác Idempotency-Key. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.userId = :userId")
    Optional<Cart> findByUserIdForUpdate(@Param("userId") long userId);

    /** Tạo Cart nếu chưa có, không lỗi khi hai request tạo cùng lúc (UNIQUE(user_id) chốt). */
    @Modifying
    @Query(value = """
            INSERT INTO carts (user_id, created_at, updated_at) VALUES (:userId, :now, :now)
            ON DUPLICATE KEY UPDATE user_id = user_id
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") long userId, @Param("now") Instant now);
}
