package com.example.lms.common;

import com.example.lms.auth.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;

import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> validationMessage(fieldError.getField()))
                .orElse("Dữ liệu gửi lên không hợp lệ.");
        return ResponseEntity.badRequest().body(error("VALIDATION_FAILED", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest() {
        return ResponseEntity.badRequest()
                .body(error("INVALID_REQUEST", "Dữ liệu gửi lên không hợp lệ."));
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidParameters(Exception exception) {
        return ResponseEntity.badRequest()
                .body(error("INVALID_PARAMETER", "Tham số yêu cầu không hợp lệ."));
    }

    private static String validationMessage(String field) {
        return switch (field) {
            case "fullName", "displayName" -> "Vui lòng nhập họ và tên.";
            case "email" -> "Vui lòng nhập địa chỉ email hợp lệ.";
            case "password", "newPassword" -> "Mật khẩu phải có từ 8 ký tự trở lên.";
            default -> "Dữ liệu gửi lên không hợp lệ.";
        };
    }

    private static ErrorResponse error(String code, String message) {
        return new ErrorResponse(new ErrorResponse.ErrorDetail(code, message, UUID.randomUUID().toString()));
    }
}
