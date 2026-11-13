package com.example.lms.user;

import com.example.lms.auth.UserPrincipal;
import com.example.lms.common.ApiResponse;
import com.example.lms.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserSummary> profile(@AuthenticationPrincipal UserPrincipal principal) {
        return new ApiResponse<>(userService.getProfile(principal));
    }

    @PatchMapping("/me")
    public ApiResponse<UserSummary> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return new ApiResponse<>(userService.updateProfile(principal, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public PageResponse<UserAdminSummary> listUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort) {
        return userService.listUsers(q, role, status, page, size, sort);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/users/{userId}/roles")
    public ApiResponse<UserAdminSummary> updateRoles(
            @PathVariable @Min(1) Long userId,
            @AuthenticationPrincipal UserPrincipal actor,
            @Valid @RequestBody SetUserRolesRequest request,
            HttpServletRequest servletRequest) {
        return new ApiResponse<>(userService.updateRoles(
                userId,
                request,
                actor,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/users/{userId}/status")
    public ApiResponse<UserAdminSummary> updateStatus(
            @PathVariable @Min(1) Long userId,
            @AuthenticationPrincipal UserPrincipal actor,
            @Valid @RequestBody UpdateUserStatusRequest request,
            HttpServletRequest servletRequest) {
        return new ApiResponse<>(userService.updateStatus(
                userId,
                request,
                actor,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")));
    }
}
