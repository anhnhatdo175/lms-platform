package com.example.lms.contract.identity;

/**
 * Điểm giao 1, A cung cấp: người đang gọi API, lấy từ access token. Controller của B không đọc
 * SecurityContext trực tiếp, nên không phụ thuộc cách A dựng Spring Security.
 */
public interface CurrentUserProvider {

    /** @throws com.example.lms.shared.exception.BusinessException {@code UNAUTHENTICATED} nếu chưa đăng nhập */
    CurrentUser current();
}
