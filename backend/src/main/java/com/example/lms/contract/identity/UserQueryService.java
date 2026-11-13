package com.example.lms.contract.identity;

import java.util.Optional;

/** Interface M3, A cung cấp: đọc thông tin User mà B cần (email cho Admin, tài khoản còn hoạt động không). */
public interface UserQueryService {

    Optional<UserSummary> findById(long userId);

    /** Tài khoản bị khóa không được học dù Enrollment còn ACTIVE (Spec 12.3). */
    boolean isActive(long userId);

    /** Admin tìm Order theo email (OpenAPI {@code GET /admin/orders?q=}). */
    Optional<Long> findIdByEmail(String email);

    java.util.Map<Long, String> emailsByIds(java.util.Collection<Long> userIds);

    record UserSummary(long id, String email, String fullName, boolean active) {
    }
}
