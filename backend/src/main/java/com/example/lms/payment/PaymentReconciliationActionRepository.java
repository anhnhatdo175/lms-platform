package com.example.lms.payment;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentReconciliationActionRepository extends JpaRepository<PaymentReconciliationAction, Long> {

    List<PaymentReconciliationAction> findByWebhookLogIdOrderByCreatedAtDescIdDesc(long webhookLogId);

    List<PaymentReconciliationAction> findByWebhookLogIdInOrderByCreatedAtDescIdDesc(Collection<Long> webhookLogIds);

    boolean existsByWebhookLogIdAndActionType(long webhookLogId, ReconciliationActionType actionType);
}
