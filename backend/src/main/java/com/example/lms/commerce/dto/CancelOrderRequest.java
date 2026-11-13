package com.example.lms.commerce.dto;

import jakarta.validation.constraints.Size;

public record CancelOrderRequest(@Size(max = 500) String reason) {
}
