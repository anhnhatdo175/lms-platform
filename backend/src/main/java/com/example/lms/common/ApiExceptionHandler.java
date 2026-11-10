package com.example.lms.common;

import com.example.lms.auth.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    private static String validationMessage(String field) {
        return switch (field) {
            case "fullName" -> "Vui lòng nhập họ và tên.";
            case "email" -> "Vui lòng nhập địa chỉ email hợp lệ.";
            case "password" -> "Mật khẩu phải có từ 12 ký tự trở lên.";
            default -> "Dữ liệu gửi lên không hợp lệ.";
        };
    }

    private static ErrorResponse error(String code, String message) {
        return new ErrorResponse(new ErrorResponse.ErrorDetail(code, message));
    }
}
