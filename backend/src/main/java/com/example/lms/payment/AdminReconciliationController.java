package com.example.lms.payment;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.payment.dto.ReconciliationDecisionRequest;
import com.example.lms.payment.dto.ReconciliationItemResponse;
import com.example.lms.payment.dto.ReconciliationMatchRequest;
import com.example.lms.shared.pagination.PageRequests;
import com.example.lms.shared.pagination.PageResponse;
import com.example.lms.shared.web.ApiResponse;

import jakarta.validation.Valid;

/** Webhook log và hàng đợi đối soát; chỉ Admin (Spec 7.4). */
@RestController
public class AdminReconciliationController {

    private final ReconciliationService reconciliation;
    private final CurrentUserProvider currentUser;

    public AdminReconciliationController(ReconciliationService reconciliation, CurrentUserProvider currentUser) {
        this.reconciliation = reconciliation;
        this.currentUser = currentUser;
    }

    @GetMapping("/admin/payment-webhooks")
    public PageResponse<ReconciliationItemResponse> webhooks(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(reconciliation.webhooks(currentUser.current(),
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"))), item -> item);
    }

    @GetMapping("/admin/reconciliation")
    public PageResponse<ReconciliationItemResponse> queue(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(reconciliation.queue(currentUser.current(),
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"))), item -> item);
    }

    @PostMapping("/admin/reconciliation/{webhookId}/approve")
    public ApiResponse<ReconciliationItemResponse> approve(@PathVariable long webhookId,
            @Valid @RequestBody ReconciliationDecisionRequest request) {
        return ApiResponse.of(reconciliation.approve(currentUser.current(), webhookId, request.reason()));
    }

    @PostMapping("/admin/reconciliation/{webhookId}/reject")
    public ApiResponse<ReconciliationItemResponse> reject(@PathVariable long webhookId,
            @Valid @RequestBody ReconciliationDecisionRequest request) {
        return ApiResponse.of(reconciliation.reject(currentUser.current(), webhookId, request.reason()));
    }

    @PostMapping("/admin/reconciliation/{webhookId}/match")
    public ApiResponse<ReconciliationItemResponse> match(@PathVariable long webhookId,
            @Valid @RequestBody ReconciliationMatchRequest request) {
        return ApiResponse.of(reconciliation.match(currentUser.current(), webhookId, request.paymentId(),
                request.reason()));
    }
}
