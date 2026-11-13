package com.example.lms.integration;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.lms.auth.UserPrincipal;
import com.example.lms.contract.identity.CurrentUser;
import com.example.lms.contract.identity.CurrentUserProvider;
import com.example.lms.shared.exception.BusinessException;
import com.example.lms.shared.exception.ErrorCode;
import com.example.lms.user.UserRole;

/**
 * Người dùng hiện tại lấy từ {@link UserPrincipal} mà {@code JwtAuthenticationFilter} đặt vào
 * SecurityContext, bao gồm toàn bộ role đã gán cho tài khoản.
 */
@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            Set<String> roles = principal.roles().stream()
                    .map(UserRole::name)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            return new CurrentUser(principal.id(), roles);
        }
        throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Authentication required");
    }
}
