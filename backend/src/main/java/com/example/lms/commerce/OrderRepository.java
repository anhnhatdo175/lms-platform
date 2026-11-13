package com.example.lms.commerce;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.lms.payment.PaymentStatus;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUserIdAndCheckoutIdempotencyKey(long userId, String checkoutIdempotencyKey);

    /** Order của người khác coi như không tồn tại (404), nên luôn tra kèm chủ sở hữu. */
    Optional<Order> findByIdAndUserId(long id, long userId);

    Page<Order> findByUserId(long userId, Pageable pageable);

    /**
     * Khóa Order. Mọi luồng chạm tiền khóa {@code orders} trước rồi mới tới {@code payments}
     * (webhook, hủy, job hết hạn, Admin duyệt), nên không có deadlock giữa chúng.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrder o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") long id);

    /**
     * Order chờ thanh toán đã quá {@code expires_at + grace} ({@code cutoff} = now − grace), trừ Order đang có
     * Payment REVIEW_REQUIRED: Admin xử lý xong mới đóng (Spec 10.6).
     */
    @Query("""
            select o.id from PurchaseOrder o
            where o.status = :pending and o.expiresAt < :cutoff
              and not exists (select p.id from Payment p where p.orderId = o.id and p.status = :review)
            order by o.expiresAt asc
            """)
    List<Long> findExpirableIds(@Param("pending") OrderStatus pending, @Param("review") PaymentStatus review,
            @Param("cutoff") Instant cutoff, Pageable pageable);

    /** Danh sách Admin; tham số {@code null} nghĩa là không lọc. */
    @Query(value = """
            select o from PurchaseOrder o
            where (:status is null or o.status = :status)
              and (:code is null or o.code = :code or o.userId = :userId)
              and (:from is null or o.createdAt >= :from)
              and (:to is null or o.createdAt < :to)
            """, countQuery = """
            select count(o) from PurchaseOrder o
            where (:status is null or o.status = :status)
              and (:code is null or o.code = :code or o.userId = :userId)
              and (:from is null or o.createdAt >= :from)
              and (:to is null or o.createdAt < :to)
            """)
    Page<Order> searchForAdmin(@Param("status") OrderStatus status, @Param("code") String code,
            @Param("userId") Long userId, @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
