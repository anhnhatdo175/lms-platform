package com.example.lms.auth;

import com.example.lms.user.User;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserRole;
import com.example.lms.user.UserStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Email hoặc mật khẩu không chính xác.";
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthenticatedSession register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new AuthException("EMAIL_ALREADY_REGISTERED", "Email này đã được đăng ký.", HttpStatus.CONFLICT);
        }
        if (request.fullName().isBlank()) {
            throw new AuthException("INVALID_NAME", "Vui lòng nhập họ và tên.", HttpStatus.BAD_REQUEST);
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AuthException("INVALID_PASSWORD", "Mật khẩu không được vượt quá 72 byte.", HttpStatus.BAD_REQUEST);
        }

        User user = new User(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserRole.STUDENT,
                UserStatus.ACTIVE);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AuthException("EMAIL_ALREADY_REGISTERED", "Email này đã được đăng ký.", HttpStatus.CONFLICT);
        }
        return createSession(user);
    }

    @Transactional
    public AuthenticatedSession login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalidCredentials();
        }
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> invalidCredentials());
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return createSession(user);
    }

    @Transactional
    public AuthenticatedSession refresh(String token) {
        Claims claims;
        try {
            claims = jwtService.parseRefreshToken(token);
        } catch (JwtException exception) {
            throw invalidRefreshToken();
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenHashForUpdate(hashToken(token))
                .orElseThrow(AuthService::invalidRefreshToken);
        Instant now = Instant.now();
        if (storedToken.getRevokedAt() != null
                || !storedToken.getExpiresAt().isAfter(now)
                || !storedToken.getUserId().toString().equals(claims.getSubject())) {
            throw invalidRefreshToken();
        }

        User user = userRepository.findById(storedToken.getUserId())
                .filter(account -> account.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(AuthService::invalidRefreshToken);
        storedToken.revoke(now);
        return createSession(user);
    }

    @Transactional
    public void logout(String token) {
        refreshTokenRepository.findByTokenHash(hashToken(token))
                .filter(refreshToken -> refreshToken.getRevokedAt() == null)
                .ifPresent(refreshToken -> refreshToken.revoke(Instant.now()));
    }

    private AuthenticatedSession createSession(User user) {
        IssuedToken accessToken = jwtService.issueAccessToken(user);
        IssuedToken refreshToken = jwtService.issueRefreshToken(user);
        refreshTokenRepository.save(new RefreshToken(
                user.getId(),
                hashToken(refreshToken.value()),
                refreshToken.expiresAt()));
        return new AuthenticatedSession(
                user,
                accessToken.value(),
                jwtProperties.accessTokenTtl().toSeconds(),
                refreshToken.value());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private static AuthException invalidCredentials() {
        return new AuthException("INVALID_CREDENTIALS", INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
    }

    private static AuthException invalidRefreshToken() {
        return new AuthException("INVALID_REFRESH_TOKEN", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
                HttpStatus.UNAUTHORIZED);
    }
}
