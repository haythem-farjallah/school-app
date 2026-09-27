package com.example.school_management.feature.auth;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Access and refresh tokens are not interchangeable, and POST /api/auth/refresh issues a new
 * access token only for an account that is still active. Runs through the real security filter
 * chain; every request uses its own client address so tests do not share rate-limit buckets.
 */
@IntegrationTest
class RefreshTokenIntegrationTest {

    private static final String INVALID_REFRESH = "Invalid or expired refresh token";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    String jwtSecret;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Test
    void accessTokenAuthenticatesProtectedApi() throws Exception {
        String email = student(false);

        mockMvc.perform(profile(login(email).path("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void refreshTokenDoesNotAuthenticateProtectedApi() throws Exception {
        String refreshToken = login(student(false)).path("refreshToken").asText();

        mockMvc.perform(profile(refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenCannotRefresh() throws Exception {
        String accessToken = login(student(false)).path("accessToken").asText();

        expectInvalidRefresh(refresh(accessToken));
    }

    @Test
    void refreshTokenIssuesWorkingAccessToken() throws Exception {
        String email = student(false);
        String refreshToken = login(email).path("refreshToken").asText();

        String newAccessToken = refreshedAccessToken(refreshToken);

        mockMvc.perform(profile(newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void suspendedAccountCannotRefreshWithEarlierToken() throws Exception {
        String email = student(false);
        String refreshToken = login(email).path("refreshToken").asText();
        setStatus(email, Status.SUSPENDED);

        String body = expectInvalidRefresh(refresh(refreshToken));

        assertThat(body).doesNotContain("accessToken", email);
    }

    @Test
    void deletedAccountCannotRefresh() throws Exception {
        String email = student(false);
        String refreshToken = login(email).path("refreshToken").asText();
        setStatus(email, Status.DELETED);

        String body = expectInvalidRefresh(refresh(refreshToken));

        assertThat(body).doesNotContain("accessToken", email, "Exception");
    }

    @Test
    void accessTokenOfDeletedAccountIsUnauthorized() throws Exception {
        String email = student(false);
        String accessToken = login(email).path("accessToken").asText();
        setStatus(email, Status.DELETED);

        String body = mockMvc.perform(profile(accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(email, "Exception");
    }

    @Test
    void accessTokenOfNonexistentAccountIsUnauthorized() throws Exception {
        String email = "nobody-" + UUID.randomUUID() + "@accounts.school.test";

        String body = mockMvc.perform(profile(signed(accessTokenFor(email))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(email, "Exception");
    }

    @Test
    void accountRequiringPasswordChangeRefreshesButStaysRestricted() throws Exception {
        String refreshToken = login(student(true)).path("refreshToken").asText();

        String newAccessToken = refreshedAccessToken(refreshToken);

        mockMvc.perform(profile(newAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("PASSWORD_CHANGE_REQUIRED"));
    }

    @Test
    void malformedRefreshTokenIsUnauthorized() throws Exception {
        expectInvalidRefresh(refresh("not-a-jwt"));
    }

    @Test
    void refreshTokenSignedWithAnotherKeyIsUnauthorized() throws Exception {
        String forged = refreshTokenFor(DevFixtureLoader.ADMIN_EMAIL, 60_000)
                .signWith(Keys.secretKeyFor(SignatureAlgorithm.HS256))
                .compact();

        expectInvalidRefresh(refresh(forged));
    }

    @Test
    void expiredRefreshTokenIsUnauthorized() throws Exception {
        String expired = refreshTokenFor(student(false), -60_000)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();

        expectInvalidRefresh(refresh(expired));
    }

    @Test
    void tokenWithoutTypeCanNeitherAuthenticateNorRefresh() throws Exception {
        String untyped = Jwts.builder()
                .setSubject(student(false))
                .claim("tokenVersion", 0)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(profile(untyped)).andExpect(status().isUnauthorized());
        expectInvalidRefresh(refresh(untyped));
    }

    @Test
    void accessTokenWithoutVersionDoesNotAuthenticate() throws Exception {
        String email = student(false);
        String unversioned = Jwts.builder()
                .setSubject(email)
                .claim("tokenType", "ACCESS")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(profile(unversioned)).andExpect(status().isUnauthorized());
        mockMvc.perform(profile(signed(accessTokenFor(email)))).andExpect(status().isOk());
    }

    @Test
    void refreshTokenWithoutVersionIsUnauthorized() throws Exception {
        String email = student(false);
        String unversioned = Jwts.builder()
                .setSubject(email)
                .claim("tokenType", "REFRESH")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();

        expectInvalidRefresh(refresh(unversioned));
        refreshedAccessToken(signed(refreshTokenFor(email, 60_000)));
    }

    @Test
    void tokensFromAnEarlierTokenVersionAreRejected() throws Exception {
        String email = student(false);
        JsonNode tokens = login(email);
        BaseUser user = userRepository.findByEmail(email).orElseThrow();
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.saveAndFlush(user);

        mockMvc.perform(profile(tokens.path("accessToken").asText())).andExpect(status().isUnauthorized());
        expectInvalidRefresh(refresh(tokens.path("refreshToken").asText()));
    }

    @Test
    void blankRefreshTokenIsRejected() throws Exception {
        mockMvc.perform(refresh(""))
                .andExpect(status().isBadRequest());
    }

    private String refreshedAccessToken(String refreshToken) throws Exception {
        String body = mockMvc.perform(refresh(refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return token.asText();
    }

    /** Asserts the generic 401 of a rejected refresh and returns the raw body. */
    private String expectInvalidRefresh(MockHttpServletRequestBuilder refresh) throws Exception {
        return mockMvc.perform(refresh)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value(INVALID_REFRESH))
                .andReturn().getResponse().getContentAsString();
    }

    private static JwtBuilder refreshTokenFor(String email, long ttlMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(email)
                .claim("tokenType", "REFRESH")
                .claim("tokenVersion", 0)
                .setIssuedAt(new Date(now - 120_000))
                .setExpiration(new Date(now + ttlMs));
    }

    private static JwtBuilder accessTokenFor(String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(email)
                .claim("tokenType", "ACCESS")
                .claim("tokenVersion", 0)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 60_000));
    }

    private String signed(JwtBuilder token) {
        return token.signWith(signingKey(), SignatureAlgorithm.HS256).compact();
    }

    /** The test profile's secret is not Base64, so the provider uses its raw bytes. */
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private String student(boolean passwordChangeRequired) {
        String email = "refresh-" + UUID.randomUUID() + "@accounts.school.test";
        Student student = new Student();
        student.setRole(UserRole.STUDENT);
        student.setEmail(email);
        student.setFirstName("Refresh");
        student.setLastName("Token");
        student.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        student.setStatus(Status.ACTIVE);
        student.setPasswordChangeRequired(passwordChangeRequired);
        student.setIsEmailVerified(true);
        studentRepository.saveAndFlush(student);
        return email;
    }

    private void setStatus(String email, Status status) {
        BaseUser user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(status);
        userRepository.saveAndFlush(user);
    }

    /** Logs in and returns the login response data (access token, refresh token, user). */
    private JsonNode login(String email) throws Exception {
        String body = mockMvc.perform(withClientAddress(post("/api/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data");
    }

    private MockHttpServletRequestBuilder refresh(String refreshToken) throws Exception {
        return withClientAddress(post("/api/auth/refresh"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken)));
    }

    private static MockHttpServletRequestBuilder profile(String token) {
        return withClientAddress(get("/api/me/profile"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static MockHttpServletRequestBuilder withClientAddress(MockHttpServletRequestBuilder request) {
        return request.with(r -> {
            r.setRemoteAddr("10.34.0." + clientAddress.incrementAndGet());
            return r;
        });
    }
}
