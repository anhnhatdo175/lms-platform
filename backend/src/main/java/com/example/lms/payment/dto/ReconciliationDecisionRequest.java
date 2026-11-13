package com.example.lms.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mọi thao tác đối soát bắt buộc có lý do (Spec 11.6). */
public record ReconciliationDecisionRequest(@NotBlank @Size(max = 2000) String reason) {
}
