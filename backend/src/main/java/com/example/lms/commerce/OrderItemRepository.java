package com.example.lms.commerce;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_IdOrderByIdAsc(long orderId);

    /**
     * Course đã nằm trong một Order khác của User đang chờ thanh toán và còn nhận tiền ({@code expiresAt > cutoff},
     * cutoff = now − grace). Checkout chặn trường hợp này để trả tiền hai đơn không sinh hai Enrollment ACTIVE.
     */
    @Query("""
            select count(i) > 0 from OrderItem i
            where i.order.userId = :userId and i.order.status = :pending
              and i.order.expiresAt > :cutoff and i.courseId in :courseIds
            """)
    boolean existsInOpenOrder(@Param("userId") long userId, @Param("pending") OrderStatus pending,
            @Param("cutoff") Instant cutoff, @Param("courseIds") Collection<Long> courseIds);
}
