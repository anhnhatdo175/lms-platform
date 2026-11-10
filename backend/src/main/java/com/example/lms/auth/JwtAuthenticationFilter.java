package com.example.lms.auth;

import com.example.lms.user.User;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserStatus;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(header.substring(7));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            String subject = jwtService.parseAccessToken(token).getSubject();
            Optional<User> user = userRepository.findById(Long.parseLong(subject));
            user.filter(account -> account.getStatus() == UserStatus.ACTIVE)
                    .ifPresent(account -> {
                        UserPrincipal principal = UserPrincipal.from(account);
                        var authentication = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                java.util.List.of(
                                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                "ROLE_" + account.getRole().name())));
                        var context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);
                    });
        } catch (JwtException | NumberFormatException exception) {
            SecurityContextHolder.clearContext();
        }
    }
}
