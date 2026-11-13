package com.example.lms.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * Luồng tiền đọc id bằng truy vấn trả giá trị đơn rồi mới khóa entity. Nếu nạp entity bằng truy vấn thường
 * trước, Hibernate sẽ trả lại đúng object cũ đó cho truy vấn {@code FOR UPDATE} sau, với dữ liệu có thể đã cũ.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    @Query("select p.orderId from Payment p where p.paymentReference = :reference")
    Optional<Long> findOrderIdByPaymentReference(@Param("reference") String paymentReference);

    @Query("select p.orderId from Payment p where p.id = :id")
    Optional<Long> findOrderIdById(@Param("id") long paymentId);

    @Query("select max(p.id) from Payment p where p.orderId = :orderId")
    Optional<Long> findLatestIdByOrderId(@Param("orderId") long orderId);

    /** Gọi sau khi đã khóa Order của Payment này (thứ tự khóa Order → Payment). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.paymentReference = :reference")
    Optional<Payment> findByPaymentReferenceForUpdate(@Param("reference") String paymentReference);

    /** Payment mới nhất của Order; mỗi Order có tối đa một Payment PENDING (ERD Mục 15, ý 2). */
    Optional<Payment> findFirstByOrderIdOrderByIdDesc(long orderId);

    boolean existsByOrderIdAndStatus(long orderId, PaymentStatus status);
}
