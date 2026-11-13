package com.example.lms.payment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, Long> {

    @Query("select l.id from PaymentWebhookLog l where l.primaryDedupKey = :key")
    Optional<Long> findIdByPrimaryDedupKey(@Param("key") String primaryDedupKey);

    /** Rỗng cả khi log không tồn tại lẫn khi log chưa gắn Payment; phân biệt bằng {@code existsById}. */
    @Query("select l.paymentId from PaymentWebhookLog l where l.id = :id")
    Optional<Long> findPaymentIdById(@Param("id") long id);

    @Query("select l.paymentReference from PaymentWebhookLog l where l.id = :id")
    Optional<String> findReferenceById(@Param("id") long id);

    /** Khóa cuối cùng, sau Order và Payment, để mọi luồng giữ cùng thứ tự khóa. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from PaymentWebhookLog l where l.id = :id")
    Optional<PaymentWebhookLog> findByIdForUpdate(@Param("id") long id);

    /** Log gốc kẹt ở trạng thái chưa xong quá lâu (lần xử lý trước lỗi): job quét để xử lý lại. */
    @Query("""
            select l.id from PaymentWebhookLog l
            where l.primaryDedupKey is not null and l.processingStatus in :statuses and l.receivedAt < :before
            order by l.receivedAt asc
            """)
    List<Long> findStuckIds(@Param("statuses") Collection<WebhookProcessingStatus> statuses,
            @Param("before") Instant before, Pageable pageable);

    /**
     * Hàng đợi đối soát (Spec 11.6): webhook UNMATCHED chưa được Admin đóng, và webhook đã đẩy Payment vào
     * REVIEW_REQUIRED mà Payment vẫn còn chờ. DUPLICATE và FAILED không vào hàng đợi.
     */
    @Query(value = """
            select l from PaymentWebhookLog l
            where (l.processingStatus = :unmatched and not exists (
                       select a.id from PaymentReconciliationAction a
                       where a.webhookLogId = l.id and a.actionType = :closing))
               or exists (select p.id from Payment p where p.paidWebhookLogId = l.id and p.status = :review)
            """, countQuery = """
            select count(l) from PaymentWebhookLog l
            where (l.processingStatus = :unmatched and not exists (
                       select a.id from PaymentReconciliationAction a
                       where a.webhookLogId = l.id and a.actionType = :closing))
               or exists (select p.id from Payment p where p.paidWebhookLogId = l.id and p.status = :review)
            """)
    Page<PaymentWebhookLog> findReconciliationQueue(@Param("unmatched") WebhookProcessingStatus unmatched,
            @Param("closing") ReconciliationActionType closing, @Param("review") PaymentStatus review,
            Pageable pageable);

    Page<PaymentWebhookLog> findAllBy(Pageable pageable);
}
