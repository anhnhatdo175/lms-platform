package com.example.lms;

import com.example.lms.user.User;
import com.example.lms.user.RoleRepository;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserRole;
import com.example.lms.user.UserStatus;
import com.example.lms.audit.AuditLogRepository;
import com.example.lms.auth.PasswordResetToken;
import com.example.lms.auth.PasswordResetTokenRepository;
import com.example.lms.course.CourseCategory;
import com.example.lms.course.CourseCategoryRepository;
import com.example.lms.file.FilePurpose;
import com.example.lms.file.FileResource;
import com.example.lms.file.FileResourceRepository;
import com.example.lms.file.FileStatus;
import com.example.lms.file.StorageProvider;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:lms;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.locations[0]=classpath:db/migration",
                "spring.flyway.locations[1]=classpath:db/migration-h2",
                "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "app.jwt.issuer=lms-test",
                "app.jwt.access-token-ttl=PT15M",
                "app.jwt.refresh-token-ttl=P14D",
                "app.auth.refresh-cookie-secure=false",
                "app.cors.allowed-origins=http://localhost:5173",
                "app.mail.enabled=true",
                "management.health.mail.enabled=false",
                "spring.mail.host=localhost",
                "app.mail.from=no-reply@example.com",
                "app.auth.password-reset-url=http://localhost:5173/"
        })
class AuthIntegrationTests {

    private static final String ORIGIN = "http://localhost:5173";
    private static final String PASSWORD = "StrongPassword-123";
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private FileResourceRepository fileResourceRepository;

    @Autowired
    private CourseCategoryRepository courseCategoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JavaMailSender mailSender;

    private HttpClient client;
    private URI baseUri;
    private String email;

    @BeforeEach
    void setUp() {
        client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        baseUri = URI.create("http://localhost:" + port);
        email = "student-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void registersRefreshesAndRevokesSession() throws Exception {
        HttpResponse<String> registration = post(
                client,
                "/api/v1/auth/register",
                "{\"displayName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);

        assertThat(registration.statusCode()).isEqualTo(201);
        assertThat(registration.body()).contains("\"roles\":[\"STUDENT\"]");

        HttpResponse<String> login = post(
                client,
                "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.headers().firstValue("set-cookie").orElse(""))
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("refreshToken=");
        assertThat(login.body()).contains("\"email\":\"" + email + "\"");
        String initialAccessToken = accessToken(login.body());
        HttpResponse<String> profile = get("/api/v1/me", initialAccessToken);
        assertThat(profile.statusCode()).isEqualTo(200);
        assertThat(profile.body()).contains(email);
        String oldRefreshToken = cookieValue(login);

        HttpResponse<String> refreshed = post(client, "/api/v1/auth/refresh", "", null);
        assertThat(refreshed.statusCode()).isEqualTo(200);
        String newAccessToken = accessToken(refreshed.body());
        assertThat(newAccessToken).isNotEqualTo(initialAccessToken);

        HttpResponse<String> replay = post(
                HttpClient.newHttpClient(),
                "/api/v1/auth/refresh",
                "",
                oldRefreshToken);
        assertThat(replay.statusCode()).isEqualTo(401);

        HttpResponse<String> logout = post(
                client,
                "/api/v1/auth/logout",
                "",
                null,
                newAccessToken);
        assertThat(logout.statusCode()).isEqualTo(204);

        HttpResponse<String> afterLogout = post(client, "/api/v1/auth/refresh", "", null);
        assertThat(afterLogout.statusCode()).isEqualTo(401);

        HttpResponse<String> anonymousLogout = post(
                HttpClient.newHttpClient(),
                "/api/v1/auth/logout",
                "",
                null);
        assertThat(anonymousLogout.statusCode()).isEqualTo(401);
    }

    @Test
    void rejectsWrongPasswordAndDisallowedRefreshOrigin() throws Exception {
        HttpResponse<String> registration = post(
                client,
                "/api/v1/auth/register",
                "{\"displayName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(registration.statusCode()).isEqualTo(201);

        HttpResponse<String> duplicateRegistration = post(
                client,
                "/api/v1/auth/register",
                "{\"displayName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(duplicateRegistration.statusCode()).isEqualTo(409);

        HttpResponse<String> weakPassword = post(
                client,
                "/api/v1/auth/register",
                "{\"displayName\":\"Nguyen Van B\",\"email\":\"other-" + email + "\",\"password\":\"short\"}",
                null);
        assertThat(weakPassword.statusCode()).isEqualTo(400);

        HttpResponse<String> badLogin = post(
                client,
                "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"WrongPassword-123\"}",
                null);
        assertThat(badLogin.statusCode()).isEqualTo(401);
        assertThat(badLogin.body()).contains("INVALID_CREDENTIALS");
        assertThat(badLogin.body()).contains("\"traceId\"");

        HttpRequest forbiddenRequest = HttpRequest.newBuilder(baseUri.resolve("/api/v1/auth/refresh"))
                .header("Origin", "https://untrusted.example")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> forbidden = HttpClient.newHttpClient()
                .send(forbiddenRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(forbidden.statusCode()).isEqualTo(403);
    }

    @Test
    void healthEndpointUsesVersionedApiBasePath() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/v1/health"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"data\"")
                .contains("\"status\":\"UP\"")
                .contains("\"database\":\"UP\"");
    }

    @Test
    void passwordResetIsGenericOneTimeAndRevokesRefreshSessions() throws Exception {
        HttpResponse<String> registration = post(
                client,
                "/api/v1/auth/register",
                "{\"displayName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(registration.statusCode()).isEqualTo(201);
        HttpResponse<String> login = post(
                client,
                "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        String oldRefreshToken = cookieValue(login);

        HttpResponse<String> forgotPassword = post(
                client,
                "/api/v1/auth/forgot-password",
                "{\"email\":\"" + email + "\"}",
                null);
        assertThat(forgotPassword.statusCode()).isEqualTo(202);
        assertThat(forgotPassword.body()).contains("Nếu email thuộc một tài khoản đang hoạt động");
        HttpResponse<String> unknownEmail = post(
                HttpClient.newHttpClient(),
                "/api/v1/auth/forgot-password",
                "{\"email\":\"unknown-" + email + "\"}",
                null);
        assertThat(unknownEmail.statusCode()).isEqualTo(202);
        assertThat(unknownEmail.body()).isEqualTo(forgotPassword.body());

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        Matcher tokenMatcher = Pattern.compile("token=([A-Za-z0-9_-]+)")
                .matcher(mailCaptor.getValue().getText());
        assertThat(tokenMatcher.find()).isTrue();
        String resetToken = tokenMatcher.group(1);

        HttpResponse<String> reset = post(
                client,
                "/api/v1/auth/reset-password",
                "{\"token\":\"" + resetToken + "\",\"newPassword\":\"NewStrongPassword-456\"}",
                null);
        assertThat(reset.statusCode()).isEqualTo(204);

        HttpResponse<String> reusedToken = post(
                client,
                "/api/v1/auth/reset-password",
                "{\"token\":\"" + resetToken + "\",\"newPassword\":\"AnotherPassword-789\"}",
                null);
        assertThat(reusedToken.statusCode()).isEqualTo(400);

        HttpResponse<String> replayedSession = post(
                HttpClient.newHttpClient(),
                "/api/v1/auth/refresh",
                "",
                oldRefreshToken);
        assertThat(replayedSession.statusCode()).isEqualTo(401);

        HttpResponse<String> oldPasswordLogin = post(
                HttpClient.newHttpClient(),
                "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(oldPasswordLogin.statusCode()).isEqualTo(401);
        HttpResponse<String> newPasswordLogin = post(
                HttpClient.newHttpClient(),
                "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"NewStrongPassword-456\"}",
                null);
        assertThat(newPasswordLogin.statusCode()).isEqualTo(200);
    }

    @Test
    void passwordChangeRequiresCurrentPasswordAndRevokesRefreshSessions() throws Exception {
        HttpResponse<String> registration = post(
                client,
                "/api/v1/auth/register",
                "{\"displayName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(registration.statusCode()).isEqualTo(201);
        HttpResponse<String> login = post(
                client,
                "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        String accessToken = accessToken(login.body());
        String refreshToken = cookieValue(login);

        HttpResponse<String> rejectedChange = post(
                client,
                "/api/v1/me/password",
                "{\"currentPassword\":\"WrongPassword-123\",\"newPassword\":\"NewStrongPassword-456\"}",
                null,
                accessToken);
        assertThat(rejectedChange.statusCode()).isEqualTo(400);
        assertThat(rejectedChange.body()).contains("INVALID_CURRENT_PASSWORD");

        HttpResponse<String> passwordChange = post(
                client,
                "/api/v1/me/password",
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NewStrongPassword-456\"}",
                null,
                accessToken);
        assertThat(passwordChange.statusCode()).isEqualTo(204);

        HttpResponse<String> revokedSession = post(
                HttpClient.newHttpClient(),
                "/api/v1/auth/refresh",
                "",
                refreshToken);
        assertThat(revokedSession.statusCode()).isEqualTo(401);
    }

    @Test
    void acceptsEightCharacterPasswordsAcrossIdentityFlows() throws Exception {
        String initialPassword = "Pass1234";
        HttpResponse<String> registration = post(
                client,
                "/api/v1/auth/register",
                "{\"displayName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\""
                        + initialPassword + "\"}",
                null);
        assertThat(registration.statusCode()).isEqualTo(201);

        HttpResponse<String> login = post(
                client,
                "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + initialPassword + "\"}",
                null);
        String accessToken = accessToken(login.body());
        HttpResponse<String> changePassword = post(
                client,
                "/api/v1/me/password",
                "{\"currentPassword\":\"" + initialPassword + "\",\"newPassword\":\"Change8!\"}",
                null,
                accessToken);
        assertThat(changePassword.statusCode()).isEqualTo(204);

        HttpResponse<String> forgotPassword = post(
                client,
                "/api/v1/auth/forgot-password",
                "{\"email\":\"" + email + "\"}",
                null);
        assertThat(forgotPassword.statusCode()).isEqualTo(202);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        Matcher tokenMatcher = Pattern.compile("token=([A-Za-z0-9_-]+)")
                .matcher(mailCaptor.getValue().getText());
        assertThat(tokenMatcher.find()).isTrue();
        HttpResponse<String> resetPassword = post(
                client,
                "/api/v1/auth/reset-password",
                "{\"token\":\"" + tokenMatcher.group(1) + "\",\"newPassword\":\"Reset88!\"}",
                null);
        assertThat(resetPassword.statusCode()).isEqualTo(204);

        HttpResponse<String> loginAfterReset = post(
                client,
                "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"Reset88!\"}",
                null);
        assertThat(loginAfterReset.statusCode()).isEqualTo(200);
    }

    @Test
    void expiredPasswordResetTokenIsRejected() throws Exception {
        HttpResponse<String> registration = post(
                client,
                "/api/v1/auth/register",
                "{\"displayName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(registration.statusCode()).isEqualTo(201);
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        String expiredToken = "expired-reset-token";
        String tokenHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(expiredToken.getBytes(StandardCharsets.UTF_8)));
        passwordResetTokenRepository.saveAndFlush(new PasswordResetToken(
                userId,
                tokenHash,
                Instant.now().minusSeconds(1)));

        HttpResponse<String> reset = post(
                client,
                "/api/v1/auth/reset-password",
                "{\"token\":\"" + expiredToken + "\",\"newPassword\":\"NewStrongPassword-456\"}",
                null);
        assertThat(reset.statusCode()).isEqualTo(400);
        assertThat(reset.body()).contains("INVALID_PASSWORD_RESET_TOKEN");
    }

    @Test
    void usersCanEditTheirOwnProfileButOnlyAdminsCanOpenDirectory() throws Exception {
        HttpResponse<String> registration = post(
                client,
                "/api/v1/auth/register",
                "{\"displayName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        String accessToken = accessToken(post(
                client,
                "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null).body());

        HttpResponse<String> profileUpdate = patch(
                "/api/v1/me",
                accessToken,
                "{\"displayName\":\"Nguyen Thi An\"}");
        assertThat(profileUpdate.statusCode()).isEqualTo(200);
        assertThat(profileUpdate.body()).contains("\"displayName\":\"Nguyen Thi An\"");
        assertThat(profileUpdate.body()).contains("\"email\":\"" + email + "\"");

        HttpResponse<String> emptyProfilePatch = patch("/api/v1/me", accessToken, "{}");
        assertThat(emptyProfilePatch.statusCode()).isEqualTo(200);
        assertThat(emptyProfilePatch.body()).contains("\"displayName\":\"Nguyen Thi An\"");

        HttpResponse<String> forbiddenDirectory = get("/api/v1/admin/users", accessToken);
        assertThat(forbiddenDirectory.statusCode()).isEqualTo(403);
    }

    @Test
    void usersCanAttachAndRemoveOnlyTheirOwnReadyAvatar() throws Exception {
        String otherEmail = "other-" + UUID.randomUUID() + "@example.com";
        HttpResponse<String> ownerRegistration = post(
                client,
                "/api/v1/auth/register",
                "{\"displayName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(ownerRegistration.statusCode()).isEqualTo(201);
        HttpResponse<String> otherRegistration = post(
                client,
                "/api/v1/auth/register",
                "{\"displayName\":\"Nguyen Thi Binh\",\"email\":\"" + otherEmail
                        + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(otherRegistration.statusCode()).isEqualTo(201);
        HttpResponse<String> login = post(
                client,
                "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(login.statusCode()).isEqualTo(200);
        String accessToken = accessToken(login.body());

        User owner = userRepository.findByEmail(email).orElseThrow();
        User otherUser = userRepository.findByEmail(otherEmail).orElseThrow();
        FileResource readyAvatar = createAvatar(owner, FilePurpose.USER_AVATAR, true);
        FileResource otherUserAvatar = createAvatar(otherUser, FilePurpose.USER_AVATAR, true);
        FileResource pendingAvatar = createAvatar(owner, FilePurpose.USER_AVATAR, false);
        FileResource readyCourseImage = createAvatar(owner, FilePurpose.COURSE_THUMBNAIL, true);

        HttpResponse<String> attach = patch(
                "/api/v1/me",
                accessToken,
                "{\"avatarResourceId\":" + readyAvatar.getId() + "}");
        assertThat(attach.statusCode()).as(attach.body()).isEqualTo(200);
        assertThat(attach.body()).contains("\"avatarResourceId\":" + readyAvatar.getId());

        HttpResponse<String> rejectOtherOwner = patch(
                "/api/v1/me",
                accessToken,
                "{\"avatarResourceId\":" + otherUserAvatar.getId() + "}");
        assertThat(rejectOtherOwner.statusCode()).isEqualTo(422);
        assertThat(rejectOtherOwner.body()).contains("INVALID_AVATAR_RESOURCE");

        HttpResponse<String> rejectPending = patch(
                "/api/v1/me",
                accessToken,
                "{\"avatarResourceId\":" + pendingAvatar.getId() + "}");
        assertThat(rejectPending.statusCode()).isEqualTo(422);

        HttpResponse<String> rejectWrongPurpose = patch(
                "/api/v1/me",
                accessToken,
                "{\"avatarResourceId\":" + readyCourseImage.getId() + "}");
        assertThat(rejectWrongPurpose.statusCode()).isEqualTo(422);

        HttpResponse<String> removeAvatar = patch(
                "/api/v1/me",
                accessToken,
                "{\"avatarResourceId\":null}");
        assertThat(removeAvatar.statusCode()).isEqualTo(200);
        assertThat(removeAvatar.body()).contains("\"avatarResourceId\":null");
        assertThat(userRepository.findByEmail(email).orElseThrow().getAvatarFileId()).isNull();
    }

    @Test
    void teachersCanOnlyDeleteTheirOwnUnusedFiles() throws Exception {
        User owner = createUserWithRole("teacher-owner-" + UUID.randomUUID() + "@example.com", UserRole.TEACHER);
        User otherTeacher = createUserWithRole(
                "teacher-other-" + UUID.randomUUID() + "@example.com", UserRole.TEACHER);
        User student = createUserWithRole("student-file-" + UUID.randomUUID() + "@example.com", UserRole.STUDENT);

        String ownerToken = loginAccessToken(owner.getEmail());
        String otherTeacherToken = loginAccessToken(otherTeacher.getEmail());
        String studentToken = loginAccessToken(student.getEmail());

        FileResource ownedFile = createAvatar(owner, FilePurpose.LESSON_RESOURCE, true);
        FileResource avatar = createAvatar(owner, FilePurpose.USER_AVATAR, true);

        HttpResponse<String> nonOwnerDelete = delete(
                "/api/v1/teacher/files/" + ownedFile.getId(), otherTeacherToken);
        assertThat(nonOwnerDelete.statusCode()).isEqualTo(403);

        HttpResponse<String> studentDelete = delete(
                "/api/v1/teacher/files/" + ownedFile.getId(), studentToken);
        assertThat(studentDelete.statusCode()).isEqualTo(403);

        HttpResponse<String> attachAvatar = patch(
                "/api/v1/me", ownerToken, "{\"avatarResourceId\":" + avatar.getId() + "}");
        assertThat(attachAvatar.statusCode()).isEqualTo(200);
        HttpResponse<String> deleteAvatarInUse = delete(
                "/api/v1/teacher/files/" + avatar.getId(), ownerToken);
        assertThat(deleteAvatarInUse.statusCode()).isEqualTo(409);
        assertThat(deleteAvatarInUse.body()).contains("FILE_RESOURCE_IN_USE");

        HttpResponse<String> removeAvatar = patch("/api/v1/me", ownerToken, "{\"avatarResourceId\":null}");
        assertThat(removeAvatar.statusCode()).isEqualTo(200);

        HttpResponse<String> deleteOwned = delete(
                "/api/v1/teacher/files/" + ownedFile.getId(), ownerToken);
        assertThat(deleteOwned.statusCode()).isEqualTo(204);
        FileResource deleted = fileResourceRepository.findById(ownedFile.getId()).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(FileStatus.DELETED);
        assertThat(deleted.getDeletedAt()).isNotNull();

        HttpResponse<String> deletedFileDetails = get("/api/v1/files/" + ownedFile.getId(), ownerToken);
        assertThat(deletedFileDetails.statusCode()).isEqualTo(200);
        assertThat(deletedFileDetails.body()).contains("\"status\":\"DELETED\"", "\"downloadUrl\":null");

        HttpResponse<String> repeatDelete = delete(
                "/api/v1/teacher/files/" + ownedFile.getId(), ownerToken);
        assertThat(repeatDelete.statusCode()).isEqualTo(204);

        HttpResponse<String> missingDelete = delete("/api/v1/teacher/files/999999999", ownerToken);
        assertThat(missingDelete.statusCode()).isEqualTo(404);
    }

    @Test
    void adminsCanManageUsersButCannotLockThemselvesOrRemoveTheLastActiveAdmin() throws Exception {
        HttpResponse<String> registration = post(
                client,
                "/api/v1/auth/register",
                "{\"displayName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        Long studentId = userRepository.findByEmail(email).orElseThrow().getId();

        String adminEmail = "admin-" + UUID.randomUUID() + "@example.com";
        User admin = userRepository.saveAndFlush(new User(
                "System Admin",
                adminEmail,
                passwordEncoder.encode(PASSWORD),
                UserStatus.ACTIVE));
        admin.replaceRoles(
                java.util.List.of(roleRepository.findByCode(UserRole.ADMIN).orElseThrow()),
                null);
        userRepository.saveAndFlush(admin);
        FileResource avatar = createAvatar(
                userRepository.findById(studentId).orElseThrow(), FilePurpose.USER_AVATAR, true);
        User student = userRepository.findById(studentId).orElseThrow();
        student.updateProfile(student.getFullName(), student.getPhoneNumber(), avatar.getId());
        userRepository.saveAndFlush(student);
        HttpResponse<String> adminLogin = post(
                client,
                "/api/v1/auth/login",
                "{\"email\":\"" + adminEmail + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        String adminToken = accessToken(adminLogin.body());

        HttpResponse<String> directory = get("/api/v1/admin/users", adminToken);
        assertThat(directory.statusCode()).isEqualTo(200);
        assertThat(directory.body())
                .contains("\"email\":\"" + email + "\"")
                .contains("\"avatarResourceId\":" + avatar.getId());

        HttpResponse<String> updateStudent = put(
                "/api/v1/admin/users/" + studentId + "/roles",
                adminToken,
                "{\"roles\":[\"TEACHER\"]}");
        assertThat(updateStudent.statusCode()).isEqualTo(200);
        assertThat(updateStudent.body()).contains("\"roles\":[\"TEACHER\"]");
        FileResource teacherFile = createAvatar(
                userRepository.findById(studentId).orElseThrow(), FilePurpose.LESSON_RESOURCE, true);
        HttpResponse<String> adminDeleteFile = delete(
                "/api/v1/teacher/files/" + teacherFile.getId(), adminToken);
        assertThat(adminDeleteFile.statusCode()).isEqualTo(204);
        assertThat(fileResourceRepository.findById(teacherFile.getId()).orElseThrow().getStatus())
                .isEqualTo(FileStatus.DELETED);

        HttpClient studentClient = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        HttpResponse<String> studentLogin = post(
                studentClient,
                "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(studentLogin.statusCode()).isEqualTo(200);
        HttpResponse<String> updateStudentStatus = patch(
                "/api/v1/admin/users/" + studentId + "/status",
                adminToken,
                "{\"status\":\"BLOCKED\",\"reason\":\"Repeated policy violations\"}");
        assertThat(updateStudentStatus.statusCode()).isEqualTo(200);
        assertThat(updateStudentStatus.body()).contains("\"status\":\"BLOCKED\"");
        HttpResponse<String> blockedRefresh = post(studentClient, "/api/v1/auth/refresh", "", null);
        assertThat(blockedRefresh.statusCode()).isEqualTo(401);

        HttpResponse<String> selfLock = patch(
                "/api/v1/admin/users/" + admin.getId() + "/status",
                adminToken,
                "{\"status\":\"INACTIVE\"}");
        assertThat(selfLock.statusCode()).isEqualTo(409);
        assertThat(selfLock.body()).contains("CANNOT_DISABLE_SELF");

        HttpResponse<String> removeLastAdmin = put(
                "/api/v1/admin/users/" + admin.getId() + "/roles",
                adminToken,
                "{\"roles\":[\"STUDENT\"]}");
        assertThat(removeLastAdmin.statusCode()).isEqualTo(409);
        assertThat(removeLastAdmin.body()).contains("LAST_ACTIVE_ADMIN");

        HttpResponse<String> auditLogs = get("/api/v1/admin/audit-logs?action=USER_ROLES_UPDATED", adminToken);
        assertThat(auditLogs.statusCode()).isEqualTo(200);
        assertThat(auditLogs.body()).contains("\"action\":\"USER_ROLES_UPDATED\"");
        HttpResponse<String> statusAuditLogs = get(
                "/api/v1/admin/audit-logs?action=USER_STATUS_UPDATED",
                adminToken);
        assertThat(statusAuditLogs.statusCode()).isEqualTo(200);
        assertThat(statusAuditLogs.body())
                .contains("\"reason\":\"Repeated policy violations\"")
                .contains("\"ipAddress\":\"127.0.0.1\"");
        assertThat(auditLogRepository.findAll().stream()
                .filter(log -> "USER_STATUS_UPDATED".equals(log.getAction()))
                .anyMatch(log -> "lms-integration-test".equals(log.getUserAgent())))
                .isTrue();
    }

    @Test
    void teachersCanAuthorCurriculumOnlyForAssignedCourses() throws Exception {
        User teacher = createUserWithRole("teacher-course-" + UUID.randomUUID() + "@example.com", UserRole.TEACHER);
        User otherTeacher = createUserWithRole(
                "teacher-course-other-" + UUID.randomUUID() + "@example.com", UserRole.TEACHER);
        String teacherToken = loginAccessToken(teacher.getEmail());
        String otherTeacherToken = loginAccessToken(otherTeacher.getEmail());
        CourseCategory category = courseCategoryRepository.saveAndFlush(new CourseCategory(
                "General",
                "general-" + UUID.randomUUID(),
                null));

        HttpResponse<String> courseResponse = post(
                client,
                "/api/v1/teacher/courses",
                "{\"title\":\"Course\",\"slug\":\"course-" + UUID.randomUUID()
                        + "\",\"categoryId\":" + category.getId()
                        + ",\"originalPrice\":0,\"salePrice\":0}",
                null,
                teacherToken);
        assertThat(courseResponse.statusCode()).as(courseResponse.body()).isEqualTo(201);
        long courseId = jsonLong(courseResponse.body(), "\"id\":(\\d+)");

        HttpResponse<String> sectionResponse = post(
                client,
                "/api/v1/teacher/courses/" + courseId + "/sections",
                "{\"title\":\"  Introduction  \",\"description\":\"Basics\",\"releaseAfterDays\":7}",
                null,
                teacherToken);
        assertThat(sectionResponse.statusCode()).as(sectionResponse.body()).isEqualTo(201);
        assertThat(sectionResponse.body())
                .contains("\"title\":\"Introduction\"")
                .contains("\"position\":1")
                .contains("\"releaseAfterDays\":7");
        long sectionId = jsonLong(sectionResponse.body(), "\"id\":(\\d+)");
        FileResource lessonResource = createAvatar(teacher, FilePurpose.LESSON_RESOURCE, true);
        FileResource otherTeacherResource = createAvatar(otherTeacher, FilePurpose.LESSON_RESOURCE, true);

        HttpResponse<String> rejectForeignResource = post(
                client,
                "/api/v1/teacher/sections/" + sectionId + "/lessons",
                "{\"title\":\"Foreign file\",\"lessonType\":\"VIDEO\",\"videoResourceId\":"
                        + otherTeacherResource.getId() + "}",
                null,
                teacherToken);
        assertThat(rejectForeignResource.statusCode()).isEqualTo(422);

        HttpResponse<String> lessonResponse = post(
                client,
                "/api/v1/teacher/sections/" + sectionId + "/lessons",
                "{\"title\":\"Lesson 1\",\"lessonType\":\"VIDEO\",\"durationSeconds\":120,"
                        + "\"completionThreshold\":80,\"videoResourceId\":" + lessonResource.getId() + "}",
                null,
                teacherToken);
        assertThat(lessonResponse.statusCode()).as(lessonResponse.body()).isEqualTo(201);
        assertThat(lessonResponse.body())
                .contains("\"position\":1")
                .contains("\"completionMethod\":\"MANUAL\"")
                .contains("\"completionThreshold\":80");
        long lessonId = jsonLong(lessonResponse.body(), "\"id\":(\\d+)");

        HttpResponse<String> updateSection = patch(
                "/api/v1/teacher/sections/" + sectionId,
                teacherToken,
                "{\"description\":null,\"releaseAfterDays\":3}");
        assertThat(updateSection.statusCode()).as(updateSection.body()).isEqualTo(200);
        assertThat(updateSection.body())
                .contains("\"description\":null")
                .contains("\"releaseAfterDays\":3");

        HttpResponse<String> updateLesson = patch(
                "/api/v1/teacher/lessons/" + lessonId,
                teacherToken,
                "{\"preview\":true,\"completionThreshold\":95}");
        assertThat(updateLesson.statusCode()).as(updateLesson.body()).isEqualTo(200);
        assertThat(updateLesson.body())
                .contains("\"preview\":true")
                .contains("\"completionThreshold\":95");

        HttpResponse<String> wrongTeacher = patch(
                "/api/v1/teacher/lessons/" + lessonId,
                otherTeacherToken,
                "{\"preview\":false}");
        assertThat(wrongTeacher.statusCode()).isEqualTo(404);

        HttpResponse<String> deleteNonEmptySection = delete(
                "/api/v1/teacher/sections/" + sectionId, teacherToken);
        assertThat(deleteNonEmptySection.statusCode()).isEqualTo(409);

        HttpResponse<String> publishLesson = patch(
                "/api/v1/teacher/lessons/" + lessonId,
                teacherToken,
                "{\"published\":true}");
        assertThat(publishLesson.statusCode()).isEqualTo(200);
        HttpResponse<String> deletePublishedLesson = delete(
                "/api/v1/teacher/lessons/" + lessonId, teacherToken);
        assertThat(deletePublishedLesson.statusCode()).isEqualTo(409);

        HttpResponse<String> unpublishLesson = patch(
                "/api/v1/teacher/lessons/" + lessonId,
                teacherToken,
                "{\"published\":false}");
        assertThat(unpublishLesson.statusCode()).isEqualTo(200);
        HttpResponse<String> deleteLesson = delete(
                "/api/v1/teacher/lessons/" + lessonId, teacherToken);
        assertThat(deleteLesson.statusCode()).isEqualTo(204);
        HttpResponse<String> deleteSection = delete(
                "/api/v1/teacher/sections/" + sectionId, teacherToken);
        assertThat(deleteSection.statusCode()).isEqualTo(204);
    }

    private HttpResponse<String> get(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private FileResource createAvatar(User uploader, FilePurpose purpose, boolean ready) {
        FileResource resource = fileResourceRepository.saveAndFlush(new FileResource(
                uploader,
                "avatar.png",
                purpose.name().toLowerCase() + "/" + UUID.randomUUID(),
                "image/png",
                128,
                StorageProvider.MINIO,
                purpose));
        if (ready) {
            resource.complete(128, "image/png", "test-checksum");
            fileResourceRepository.saveAndFlush(resource);
        }
        return resource;
    }

    private User createUserWithRole(String userEmail, UserRole role) {
        User user = userRepository.saveAndFlush(new User(
                "Test " + role.name(),
                userEmail,
                passwordEncoder.encode(PASSWORD),
                UserStatus.ACTIVE));
        user.replaceRoles(
                java.util.List.of(roleRepository.findByCode(role).orElseThrow()),
                null);
        return userRepository.saveAndFlush(user);
    }

    private String loginAccessToken(String userEmail) throws Exception {
        HttpResponse<String> login = post(
                client,
                "/api/v1/auth/login",
                "{\"email\":\"" + userEmail + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(login.statusCode()).as(login.body()).isEqualTo(200);
        return accessToken(login.body());
    }

    private static long jsonLong(String json, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(json);
        assertThat(matcher.find()).as(json).isTrue();
        return Long.parseLong(matcher.group(1));
    }

    private HttpResponse<String> delete(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> patch(String path, String accessToken, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Content-Type", "application/json")
                .header("User-Agent", "lms-integration-test")
                .header("Authorization", "Bearer " + accessToken)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String accessToken, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(
            HttpClient httpClient,
            String path,
            String body,
            String refreshCookie) throws Exception {
        return post(httpClient, path, body, refreshCookie, null);
    }

    private HttpResponse<String> post(
            HttpClient httpClient,
            String path,
            String body,
            String refreshCookie,
            String accessToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Origin", ORIGIN);
        if (body.isEmpty()) {
            request.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
        }
        if (refreshCookie != null) {
            request.header("Cookie", "refreshToken=" + refreshCookie);
        }
        if (accessToken != null) {
            request.header("Authorization", "Bearer " + accessToken);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String accessToken(String body) {
        Matcher matcher = ACCESS_TOKEN_PATTERN.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static String cookieValue(HttpResponse<String> response) {
        String cookie = response.headers().firstValue("set-cookie").orElseThrow();
        Matcher matcher = Pattern.compile("refreshToken=([^;]+)").matcher(cookie);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
