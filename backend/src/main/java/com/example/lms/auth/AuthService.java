package com.example.lms.auth;

import com.example.lms.user.User;
import com.example.lms.user.Role;
import com.example.lms.user.RoleRepository;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserRole;
import com.example.lms.user.UserStatus;
import com.example.lms.user.UserSummary;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Email hoặc mật khẩu không chính xác.";
    private static final String INVALID_PASSWORD_RESET_TOKEN = "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.";
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordResetEmailService passwordResetEmailService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            PasswordResetEmailService passwordResetEmailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.passwordResetEmailService = passwordResetEmailService;
    }

    @Transactional
    public UserSummary register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new AuthException("EMAIL_ALREADY_REGISTERED", "Email này đã được đăng ký.", HttpStatus.CONFLICT);
        }
        if (request.displayName().isBlank()) {
            throw new AuthException("INVALID_NAME", "Vui lòng nhập họ và tên.", HttpStatus.BAD_REQUEST);
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AuthException("INVALID_PASSWORD", "Mật khẩu không được vượt quá 72 byte.", HttpStatus.BAD_REQUEST);
        }

        User user = new User(
                request.displayName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserStatus.ACTIVE);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailConstraintViolation(exception)) {
                throw new AuthException("EMAIL_ALREADY_REGISTERED", "Email này đã được đăng ký.", HttpStatus.CONFLICT);
            }
            throw exception;
        }
        Role studentRole = roleRepository.findByCode(UserRole.STUDENT)
                .orElseThrow(() -> new IllegalStateException("The STUDENT role has not been seeded."));
        user.replaceRoles(List.of(studentRole), null);
        userRepository.saveAndFlush(user);
        return UserSummary.from(user);
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
        user.recordLogin(Instant.now());
        return createSession(user);
    }

    @Transactional(noRollbackFor = AuthException.class)
    public AuthenticatedSession refresh(String token) {
        Claims claims;
        try {
            claims = jwtService.parseRefreshToken(token);
        } catch (JwtException exception) {
            throw invalidRefreshToken();
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenHashForUpdate(TokenHash.sha256(token))
                .orElseThrow(AuthService::invalidRefreshToken);
        Instant now = Instant.now();
        if (storedToken.getRevokedAt() != null
                || !storedToken.getExpiresAt().isAfter(now)
                || !storedToken.getUserId().toString().equals(claims.getSubject())) {
            if (storedToken.getFamilyId() != null) {
                refreshTokenRepository.revokeFamily(storedToken.getFamilyId(), now, "refresh-token-reuse");
            }
            throw invalidRefreshToken();
        }

        User user = userRepository.findById(storedToken.getUserId()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            if (storedToken.getFamilyId() != null) {
                refreshTokenRepository.revokeFamily(storedToken.getFamilyId(), now, "account-inactive");
            } else {
                storedToken.revoke(now, "account-inactive");
            }
            throw invalidRefreshToken();
        }
        storedToken.revoke(now, "rotated");
        String familyId = storedToken.getFamilyId() == null
                ? UUID.randomUUID().toString()
                : storedToken.getFamilyId();
        storedToken.assignFamilyId(familyId);
        return createSession(user, familyId);
    }

    @Transactional
    public void logout(String token) {
        refreshTokenRepository.findByTokenHash(TokenHash.sha256(token))
                .filter(refreshToken -> refreshToken.getRevokedAt() == null)
                .ifPresent(refreshToken -> {
                    if (refreshToken.getFamilyId() != null) {
                        refreshTokenRepository.revokeFamily(
                                refreshToken.getFamilyId(), Instant.now(), "logout");
                    } else {
                        refreshToken.revoke(Instant.now(), "logout");
                    }
                });
    }

    @Transactional
    public PasswordResetAccepted requestPasswordReset(ForgotPasswordRequest request) {
        passwordResetEmailService.assertConfigured();
        userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> {
                    Instant now = Instant.now();
                    passwordResetTokenRepository.invalidateUnusedByUserId(user.getId(), now);
                    String token = newPasswordResetToken();
                    PasswordResetToken resetToken = passwordResetTokenRepository.save(
                            new PasswordResetToken(
                                    user.getId(),
                                    TokenHash.sha256(token),
                                    now.plus(PASSWORD_RESET_TTL)));
                    if (!passwordResetEmailService.sendResetLink(user.getEmail(), user.getFullName(), token)) {
                        resetToken.markUsed(now);
                    }
                });
        return new PasswordResetAccepted(
                true,
                "Nếu email thuộc một tài khoản đang hoạt động, hướng dẫn đặt lại mật khẩu sẽ được gửi.");
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Instant now = Instant.now();
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHashForUpdate(TokenHash.sha256(request.token()))
                .orElseThrow(AuthService::invalidPasswordResetToken);
        if (resetToken.getUsedAt() != null || !resetToken.getExpiresAt().isAfter(now)) {
            throw invalidPasswordResetToken();
        }

        User user = userRepository.findById(resetToken.getUserId())
                .filter(account -> account.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(AuthService::invalidPasswordResetToken);
        validatePasswordLength(request.newPassword());
        user.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.markUsed(now);
        passwordResetTokenRepository.invalidateUnusedByUserId(user.getId(), now);
        refreshTokenRepository.revokeOtherTokens(user.getId(), null, now, "password-reset");
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = userRepository.findById(principal.id())
                .filter(account -> account.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(AuthService::invalidCredentials);
        if (request.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthException(
                    "INVALID_CURRENT_PASSWORD",
                    "Mật khẩu hiện tại không chính xác.",
                    HttpStatus.BAD_REQUEST);
        }
        validatePasswordLength(request.newPassword());
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new AuthException(
                    "PASSWORD_UNCHANGED",
                    "Mật khẩu mới phải khác mật khẩu hiện tại.",
                    HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        user.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
        passwordResetTokenRepository.invalidateUnusedByUserId(user.getId(), now);
        refreshTokenRepository.revokeOtherTokens(user.getId(), null, now, "password-changed");
    }

    private AuthenticatedSession createSession(User user) {
        return createSession(user, UUID.randomUUID().toString());
    }

    private AuthenticatedSession createSession(User user, String familyId) {
        IssuedToken accessToken = jwtService.issueAccessToken(user);
        IssuedToken refreshToken = jwtService.issueRefreshToken(user);
        refreshTokenRepository.save(new RefreshToken(
                user.getId(),
                TokenHash.sha256(refreshToken.value()),
                refreshToken.expiresAt(),
                familyId));
        return new AuthenticatedSession(
                user,
                accessToken.value(),
                jwtProperties.accessTokenTtl().toSeconds(),
                refreshToken.value());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String newPasswordResetToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private static void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AuthException(
                    "INVALID_PASSWORD",
                    "Mật khẩu không được vượt quá 72 byte.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private static boolean isEmailConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) {
                return "uk_users_email".equalsIgnoreCase(violation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static AuthException invalidCredentials() {
        return new AuthException("INVALID_CREDENTIALS", INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
    }

    private static AuthException invalidRefreshToken() {
        return new AuthException("INVALID_REFRESH_TOKEN", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
                HttpStatus.UNAUTHORIZED);
    }

    private static AuthException invalidPasswordResetToken() {
        return new AuthException("INVALID_PASSWORD_RESET_TOKEN", INVALID_PASSWORD_RESET_TOKEN, HttpStatus.BAD_REQUEST);
    }
}
