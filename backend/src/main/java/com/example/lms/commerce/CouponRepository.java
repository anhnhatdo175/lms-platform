package com.example.lms.commerce;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /** {@code code} phải đã qua {@link Coupon#normalizeCode}. */
    Optional<Coupon> findByCode(String code);

    /**
     * Tăng lượt dùng bằng một câu UPDATE nguyên tử, không đọc-sửa-ghi, nên hai Payment thành công cùng lúc
     * không làm mất lượt nào. Không chặn vượt {@code total_usage_limit}: Spec 10.2 chấp nhận vượt nhẹ.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE coupons SET used_count = used_count + 1 WHERE id = :id", nativeQuery = true)
    int incrementUsedCount(@Param("id") long id);

    @Query("""
            select c from Coupon c
            where (:status is null or c.status = :status)
              and (:q is null or c.code like concat('%', :q, '%') or c.name like concat('%', :q, '%'))
            """)
    Page<Coupon> search(@Param("status") CouponStatus status, @Param("q") String q, Pageable pageable);
}
