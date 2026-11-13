package com.example.lms.integration;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.lms.common.ErrorResponse;
import com.example.lms.common.ErrorResponse.ErrorDetail;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;

/**
 * Lỗi nghiệp vụ của B theo đúng định dạng {@link ErrorResponse} của A. Chỉ áp cho controller của B, nên
 * không đổi cách {@code ApiExceptionHandler} của A trả lỗi cho phần của A.
 */
@RestControllerAdvice(basePackages = { "com.example.lms.commerce", "com.example.lms.payment",
        "com.example.lms.enrollment", "com.example.lms.exam" })
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BusinessExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(exception.httpStatus())
                .body(new ErrorResponse(new ErrorDetail(exception.code().name(), exception.getMessage())));
    }

    /** Thiếu {@code Idempotency-Key} khi checkout. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(new ErrorDetail(
                ErrorCode.VALIDATION_ERROR.name(), "Missing header " + exception.getHeaderName())));
    }
}
