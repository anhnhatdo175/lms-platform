package com.example.lms.auth;

import com.example.lms.user.Role;
import com.example.lms.user.RoleRepository;
import com.example.lms.user.User;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserRole;
import com.example.lms.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Component
@Profile("dev")
public class DevelopmentAdminBootstrap implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DevelopmentAdminBootstrap.class);

    private final String email;
    private final String password;
    private final String displayName;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DevelopmentAdminBootstrap(
            @Value("${app.auth.bootstrap-admin.email:}") String email,
            @Value("${app.auth.bootstrap-admin.password:}") String password,
            @Value("${app.auth.bootstrap-admin.display-name:LMS Administrator}") String displayName,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
        this.displayName = displayName.trim();
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isEmpty() && password.isEmpty()) {
            logger.info("No development administrator configured; no default admin account exists.");
            return;
        }
        if (email.isEmpty() || password.isEmpty()) {
            throw new IllegalStateException(
                    "Set both LMS_BOOTSTRAP_ADMIN_EMAIL and LMS_BOOTSTRAP_ADMIN_PASSWORD to bootstrap a development admin.");
        }
        if (password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || displayName.isEmpty()) {
            throw new IllegalStateException("Development administrator credentials do not meet the required limits.");
        }

        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            if (existing.hasRole(UserRole.ADMIN)) {
                return;
            }
            throw new IllegalStateException(
                    "The configured development administrator email already belongs to a non-admin account.");
        }

        Role adminRole = roleRepository.findByCode(UserRole.ADMIN)
                .orElseThrow(() -> new IllegalStateException("The ADMIN role has not been seeded."));
        User admin = userRepository.saveAndFlush(new User(
                displayName,
                email,
                passwordEncoder.encode(password),
                UserStatus.ACTIVE));
        admin.replaceRoles(List.of(adminRole), null);
        userRepository.saveAndFlush(admin);
        logger.info("Development administrator account bootstrapped.");
    }
}
