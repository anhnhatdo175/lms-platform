package com.example.lms;

import com.example.lms.user.User;
import com.example.lms.user.UserRepository;
import com.example.lms.user.UserRole;
import com.example.lms.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:lms;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=validate",
                "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "app.jwt.issuer=lms-test",
                "app.jwt.access-token-ttl=PT15M",
                "app.jwt.refresh-token-ttl=P14D",
                "app.auth.refresh-cookie-secure=false",
                "app.cors.allowed-origins=http://localhost:5173"
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
    private PasswordEncoder passwordEncoder;

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
                "/api/auth/register",
                "{\"fullName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);

        assertThat(registration.statusCode()).isEqualTo(200);
        assertThat(registration.body()).contains("\"role\":\"STUDENT\"");
        String initialAccessToken = accessToken(registration.body());
        assertThat(registration.headers().firstValue("set-cookie").orElse(""))
                .contains("HttpOnly")
                .contains("SameSite=Strict");

        HttpResponse<String> profile = get("/api/auth/me", initialAccessToken);
        assertThat(profile.statusCode()).isEqualTo(200);
        assertThat(profile.body()).contains(email);

        HttpResponse<String> login = post(
                client,
                "/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("\"email\":\"" + email + "\"");
        String oldRefreshToken = cookieValue(login);

        HttpResponse<String> refreshed = post(client, "/api/auth/refresh", "", null);
        assertThat(refreshed.statusCode()).isEqualTo(200);
        String newAccessToken = accessToken(refreshed.body());
        assertThat(newAccessToken).isNotEqualTo(initialAccessToken);

        HttpResponse<String> replay = post(
                HttpClient.newHttpClient(),
                "/api/auth/refresh",
                "",
                oldRefreshToken);
        assertThat(replay.statusCode()).isEqualTo(401);

        HttpResponse<String> logout = post(
                client,
                "/api/auth/logout",
                "",
                null,
                newAccessToken);
        assertThat(logout.statusCode()).isEqualTo(204);

        HttpResponse<String> afterLogout = post(client, "/api/auth/refresh", "", null);
        assertThat(afterLogout.statusCode()).isEqualTo(401);
    }

    @Test
    void rejectsWrongPasswordAndDisallowedRefreshOrigin() throws Exception {
        HttpResponse<String> registration = post(
                client,
                "/api/auth/register",
                "{\"fullName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(registration.statusCode()).isEqualTo(200);

        HttpResponse<String> duplicateRegistration = post(
                client,
                "/api/auth/register",
                "{\"fullName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        assertThat(duplicateRegistration.statusCode()).isEqualTo(409);

        HttpResponse<String> weakPassword = post(
                client,
                "/api/auth/register",
                "{\"fullName\":\"Nguyen Van B\",\"email\":\"other-" + email + "\",\"password\":\"short\"}",
                null);
        assertThat(weakPassword.statusCode()).isEqualTo(400);

        HttpResponse<String> badLogin = post(
                client,
                "/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"WrongPassword-123\"}",
                null);
        assertThat(badLogin.statusCode()).isEqualTo(401);
        assertThat(badLogin.body()).contains("INVALID_CREDENTIALS");

        HttpRequest forbiddenRequest = HttpRequest.newBuilder(baseUri.resolve("/api/auth/refresh"))
                .header("Origin", "https://untrusted.example")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> forbidden = HttpClient.newHttpClient()
                .send(forbiddenRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(forbidden.statusCode()).isEqualTo(403);
    }

    @Test
    void usersCanEditTheirOwnProfileButOnlyAdminsCanOpenDirectory() throws Exception {
        HttpResponse<String> registration = post(
                client,
                "/api/auth/register",
                "{\"fullName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        String accessToken = accessToken(registration.body());

        HttpResponse<String> profileUpdate = patch(
                "/api/users/me",
                accessToken,
                "{\"fullName\":\"Nguyen Thi An\"}");
        assertThat(profileUpdate.statusCode()).isEqualTo(200);
        assertThat(profileUpdate.body()).contains("\"fullName\":\"Nguyen Thi An\"");
        assertThat(profileUpdate.body()).contains("\"email\":\"" + email + "\"");

        HttpResponse<String> forbiddenDirectory = get("/api/users", accessToken);
        assertThat(forbiddenDirectory.statusCode()).isEqualTo(403);
    }

    @Test
    void adminsCanManageUsersButCannotLockThemselvesOrRemoveTheLastActiveAdmin() throws Exception {
        HttpResponse<String> registration = post(
                client,
                "/api/auth/register",
                "{\"fullName\":\"Nguyen Van An\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        Long studentId = userRepository.findByEmail(email).orElseThrow().getId();

        String adminEmail = "admin-" + UUID.randomUUID() + "@example.com";
        User admin = userRepository.saveAndFlush(new User(
                "System Admin",
                adminEmail,
                passwordEncoder.encode(PASSWORD),
                UserRole.ADMIN,
                UserStatus.ACTIVE));
        HttpResponse<String> adminLogin = post(
                client,
                "/api/auth/login",
                "{\"email\":\"" + adminEmail + "\",\"password\":\"" + PASSWORD + "\"}",
                null);
        String adminToken = accessToken(adminLogin.body());

        HttpResponse<String> directory = get("/api/users", adminToken);
        assertThat(directory.statusCode()).isEqualTo(200);
        assertThat(directory.body()).contains("\"email\":\"" + email + "\"");

        HttpResponse<String> updateStudent = patch(
                "/api/users/" + studentId,
                adminToken,
                "{\"role\":\"TEACHER\",\"status\":\"LOCKED\"}");
        assertThat(updateStudent.statusCode()).isEqualTo(200);
        assertThat(updateStudent.body()).contains("\"role\":\"TEACHER\"");
        assertThat(updateStudent.body()).contains("\"status\":\"LOCKED\"");

        HttpResponse<String> selfLock = patch(
                "/api/users/" + admin.getId(),
                adminToken,
                "{\"status\":\"LOCKED\"}");
        assertThat(selfLock.statusCode()).isEqualTo(409);
        assertThat(selfLock.body()).contains("CANNOT_LOCK_SELF");

        HttpResponse<String> removeLastAdmin = patch(
                "/api/users/" + admin.getId(),
                adminToken,
                "{\"role\":\"STUDENT\"}");
        assertThat(removeLastAdmin.statusCode()).isEqualTo(409);
        assertThat(removeLastAdmin.body()).contains("LAST_ACTIVE_ADMIN");
    }

    private HttpResponse<String> get(String path, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> patch(String path, String accessToken, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
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
            request.header("Cookie", "lms_refresh=" + refreshCookie);
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
        Matcher matcher = Pattern.compile("lms_refresh=([^;]+)").matcher(cookie);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
