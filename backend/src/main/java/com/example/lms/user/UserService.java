package com.example.lms.user;

import com.example.lms.auth.AuthException;
import com.example.lms.auth.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserSummary getProfile(UserPrincipal principal) {
        return UserSummary.from(requireUser(principal.id()));
    }

    @Transactional
    public UserSummary updateProfile(UserPrincipal principal, UpdateProfileRequest request) {
        User user = requireUser(principal.id());
        user.updateProfile(request.fullName().trim());
        return UserSummary.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserDirectoryResponse listUsers(
            String query,
            UserRole role,
            UserStatus status,
            int page,
            int size) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        Page<User> users = userRepository.searchUsers(
                normalizedQuery,
                role,
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return UserDirectoryResponse.from(
                users,
                userRepository.countByStatus(UserStatus.ACTIVE),
                userRepository.countByStatus(UserStatus.LOCKED),
                userRepository.countByRole(UserRole.STUDENT),
                userRepository.countByRole(UserRole.TEACHER),
                userRepository.countByRole(UserRole.ADMIN));
    }

    @Transactional
    public UserSummary updateUser(Long userId, UpdateUserRequest request, UserPrincipal actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "Không tìm thấy người dùng.", HttpStatus.NOT_FOUND));
        if (request.role() == null && request.status() == null
                && (request.fullName() == null || request.fullName().isBlank())) {
            throw new AuthException("NO_CHANGES", "Chưa có thông tin nào được thay đổi.", HttpStatus.BAD_REQUEST);
        }
        if (request.status() == UserStatus.LOCKED && actor.id().equals(userId)) {
            throw new AuthException("CANNOT_LOCK_SELF", "Bạn không thể tự khóa tài khoản của mình.", HttpStatus.CONFLICT);
        }

        UserRole nextRole = request.role() == null ? user.getRole() : request.role();
        UserStatus nextStatus = request.status() == null ? user.getStatus() : request.status();
        boolean removingActiveAdmin = user.getRole() == UserRole.ADMIN
                && user.getStatus() == UserStatus.ACTIVE
                && (nextRole != UserRole.ADMIN || nextStatus != UserStatus.ACTIVE);
        if (removingActiveAdmin
                && userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new AuthException(
                    "LAST_ACTIVE_ADMIN",
                    "Không thể thay đổi tài khoản quản trị viên đang hoạt động cuối cùng.",
                    HttpStatus.CONFLICT);
        }

        if (request.fullName() != null) {
            if (request.fullName().isBlank()) {
                throw new AuthException("INVALID_NAME", "Họ và tên không được để trống.", HttpStatus.BAD_REQUEST);
            }
            user.updateProfile(request.fullName().trim());
        }
        if (request.role() != null) user.updateRole(request.role());
        if (request.status() != null) user.updateStatus(request.status());
        return UserSummary.from(userRepository.save(user));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "Không tìm thấy người dùng.", HttpStatus.NOT_FOUND));
    }
}
