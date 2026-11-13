package com.example.lms.shared.web;

/**
 * Envelope thành công {@code {"data": ...}} (DEC-008). Chủ sở hữu: A; bản tạm để controller của B biên dịch
 * được, lấy bản của A khi có.
 */
public record ApiResponse<T>(T data) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
