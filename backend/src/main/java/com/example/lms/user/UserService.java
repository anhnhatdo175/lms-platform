package com.example.lms.user;

import com.example.lms.audit.AuditLogService;
import com.example.lms.auth.AuthException;
import com.example.lms.auth.RefreshTokenRepository;
import com.example.lms.auth.UserPrincipal;
import com.example.lms.common.PageMeta;
import com.example.lms.common.PageResponse;
import com.example.lms.file.FileStorageService;
import tools.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FileStorageService fileStorageService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuditLogService auditLogService,
            RefreshTokenRepository refreshTokenRepository,
            FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public UserSummary getProfile(UserPrincipal principal) {
        return UserSummary.from(requireUser(principal.id()));
    }

    @Transactional
    public UserSummary updateProfile(UserPrincipal principal, UpdateProfileRequest request) {
        User user = requireUser(principal.id());
        String displayName = user.getFullName();
        if (request.getDisplayName() != null) {
            displayName = request.getDisplayName().trim();
            if (displayName.isEmpty()) {
                throw new AuthException("INVALID_NAME", "Họ và tên không được để trống.", HttpStatus.BAD_REQUEST);
            }
        }

        Long avatarResourceId = user.getAvatarFileId();
        JsonNode avatarChange = request.getAvatarResourceId();
        if (request.isAvatarResourceIdProvided()) {
            if (avatarChange == null || avatarChange.isNull()) {
                avatarResourceId = null;
            } else if (avatarChange.isIntegralNumber()
                    && avatarChange.canConvertToLong()
                    && avatarChange.longValue() > 0) {
                avatarResourceId = avatarChange.longValue();
                fileStorageService.validateAvatar(avatarResourceId, principal.id());
            } else {
                throw new AuthException(
                        "INVALID_AVATAR_RESOURCE",
                        "Mã ảnh đại diện không hợp lệ.",
                        HttpStatus.BAD_REQUEST);
            }
        }
        if (request.getDisplayName() == null && !request.isAvatarResourceIdProvided()) {
            return UserSummary.from(user);
        }
        user.updateProfile(displayName, user.getPhoneNumber(), avatarResourceId);
        return UserSummary.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserAdminSummary> listUsers(
            String query,
            UserRole role,
            UserStatus status,
            int page,
            int size,
            String sortExpression) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        Page<User> users = userRepository.searchUsers(
                normalizedQuery,
                role,
                status,
                PageRequest.of(page, size, parseSort(sortExpression)));
        return new PageResponse<>(
                users.map(UserAdminSummary::from).getContent(),
                new PageMeta(users.getNumber(), users.getSize(), users.getTotalElements(), users.getTotalPages()));
    }

    @Transactional
    public UserAdminSummary updateRoles(
            Long userId,
            SetUserRolesRequest request,
            UserPrincipal actor,
            String ipAddress,
            String userAgent) {
        if (request.roles().size() != Set.copyOf(request.roles()).size()) {
            throw new AuthException("DUPLICATE_ROLES", "Không thể gán một vai trò nhiều lần.", HttpStatus.BAD_REQUEST);
        }
        User user = requireUser(userId);
        Set<UserRole> nextRoles = Set.copyOf(request.roles());
        if (nextRoles.isEmpty()) {
            throw new AuthException("ROLE_REQUIRED", "Tài khoản phải có ít nhất một vai trò.", HttpStatus.BAD_REQUEST);
        }
        User actorUser = requireUser(actor.id());
        protectLastActiveAdmin(user, nextRoles, user.getStatus());

        Set<UserRole> previousRoles = user.getRoles();
        if (previousRoles.equals(nextRoles)) {
            return UserAdminSummary.from(user);
        }
        List<Role> roles = roleRepository.findAllByCodeIn(nextRoles);
        if (roles.size() != nextRoles.size()) {
            throw new IllegalStateException("One or more requested roles are missing from the role catalog.");
        }
        user.replaceRoles(roles, actorUser);
        userRepository.save(user);
        auditLogService.record(
                actorUser,
                "USER_ROLES_UPDATED",
                "USER",
                user.getId(),
                ipAddress,
                userAgent,
                Map.of("roles", previousRoles.stream().map(Enum::name).sorted().toList()),
                Map.of("roles", nextRoles.stream().map(Enum::name).sorted().toList()),
                null);
        return UserAdminSummary.from(user);
    }

    @Transactional
    public UserAdminSummary updateStatus(
            Long userId,
            UpdateUserStatusRequest request,
            UserPrincipal actor,
            String ipAddress,
            String userAgent) {
        User user = requireUser(userId);
        User actorUser = requireUser(actor.id());
        if (user.getId().equals(actor.id()) && request.status() != UserStatus.ACTIVE) {
            throw new AuthException("CANNOT_DISABLE_SELF", "Bạn không thể tự vô hiệu hóa tài khoản của mình.", HttpStatus.CONFLICT);
        }
        if (user.getStatus() == request.status()) {
            return UserAdminSummary.from(user);
        }
        protectLastActiveAdmin(user, user.getRoles(), request.status());
        UserStatus previousStatus = user.getStatus();
        user.updateStatus(request.status());
        userRepository.save(user);
        if (request.status() != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeOtherTokens(
                    user.getId(), null, java.time.Instant.now(), "account-status-changed");
        }
        auditLogService.record(
                actorUser,
                "USER_STATUS_UPDATED",
                "USER",
                user.getId(),
                ipAddress,
                userAgent,
                Map.of("status", previousStatus.name()),
                Map.of("status", request.status().name()),
                request.reason());
        return UserAdminSummary.from(user);
    }

    private void protectLastActiveAdmin(User user, Collection<UserRole> nextRoles, UserStatus nextStatus) {
        boolean removingActiveAdmin = user.hasRole(UserRole.ADMIN)
                && user.getStatus() == UserStatus.ACTIVE
                && (!nextRoles.contains(UserRole.ADMIN) || nextStatus != UserStatus.ACTIVE);
        if (removingActiveAdmin
                && userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new AuthException(
                    "LAST_ACTIVE_ADMIN",
                    "Không thể thay đổi tài khoản quản trị viên đang hoạt động cuối cùng.",
                    HttpStatus.CONFLICT);
        }
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        "USER_NOT_FOUND",
                        "Không tìm thấy người dùng.",
                        HttpStatus.NOT_FOUND));
    }

    private static Sort parseSort(String expression) {
        if (expression == null || expression.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        }
        String[] parts = expression.split(",", -1);
        if (parts.length > 2) {
            throw new AuthException("INVALID_SORT", "Tham số sắp xếp không hợp lệ.", HttpStatus.BAD_REQUEST);
        }
        String property = switch (parts[0]) {
            case "createdAt", "lastLoginAt", "email", "status" -> parts[0];
            case "fullName", "displayName" -> "fullName";
            default -> throw new AuthException("INVALID_SORT", "Tham số sắp xếp không hợp lệ.", HttpStatus.BAD_REQUEST);
        };
        Sort.Direction direction;
        try {
            direction = parts.length == 1 ? Sort.Direction.ASC : Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new AuthException("INVALID_SORT", "Tham số sắp xếp không hợp lệ.", HttpStatus.BAD_REQUEST);
        }
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
