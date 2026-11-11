package com.example.lms.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthOriginFilter extends OncePerRequestFilter {

    private final Set<String> allowedOrigins;

    public AuthOriginFilter(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !("POST".equals(request.getMethod())
                && ("/api/v1/auth/login".equals(path)
                        || "/api/v1/auth/register".equals(path)
                        || "/api/v1/auth/refresh".equals(path)
                        || "/api/v1/auth/logout".equals(path)
                        || "/api/v1/auth/forgot-password".equals(path)
                        || "/api/v1/auth/reset-password".equals(path)
                        || "/api/v1/me/password".equals(path)));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin == null || !allowedOrigins.contains(origin)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"error\":{\"code\":\"ORIGIN_NOT_ALLOWED\",\"message\":\"Nguồn yêu cầu không được phép.\",\"traceId\":\""
                            + java.util.UUID.randomUUID() + "\"}}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
