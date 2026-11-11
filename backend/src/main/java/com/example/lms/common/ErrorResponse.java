package com.example.lms.common;

public record ErrorResponse(ErrorDetail error) {
    public record ErrorDetail(String code, String message, String traceId) {
    }
}
