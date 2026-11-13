package com.example.lms.integration.pending;

import org.springframework.stereotype.Component;

import com.example.lms.contract.identity.AccessPolicy;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserRole;

/**
 * BẢN TẠM của B để ứng dụng khởi động được khi A chưa có {@code course_teachers}: chỉ Admin quản lý được Course, Teacher bị chặn. A thay bằng bản kiểm
 * tra Teacher được gán vào Course.
 */
@Component
public class PendingAccessPolicy implements AccessPolicy {

    private final UserRepository users;

    public PendingAccessPolicy(UserRepository users) {
        this.users = users;
    }

    @Override
    public boolean canManageCourse(long userId, long courseId) {
        return users.findById(userId).map(u -> u.getRole() == UserRole.ADMIN).orElse(false);
    }
}
