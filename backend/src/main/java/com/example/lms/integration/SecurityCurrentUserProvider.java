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

/**
 * Người dùng hiện tại lấy từ {@link UserPrincipal} mà {@code JwtAuthenticationFilter} của A đặt vào
 * SecurityContext. A lưu một role cho mỗi User, nên tập role ở đây có đúng một phần tử.
 */
@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return new CurrentUser(principal.id(), Set.of(principal.role().name()));
        }
        throw new BusinessException(ErrorCode.UNAUTHENTICATED, "Authentication required");
    }
}
