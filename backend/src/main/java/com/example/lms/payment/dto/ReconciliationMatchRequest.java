package com.example.lms.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReconciliationMatchRequest(@NotNull @Positive Long paymentId, @NotBlank @Size(max = 2000) String reason) {
}
