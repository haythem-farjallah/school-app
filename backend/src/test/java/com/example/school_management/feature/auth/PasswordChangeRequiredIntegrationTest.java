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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accounts flagged passwordChangeRequired can log in, but their token only authenticates the
 * change of their own password until it is done. A successful change revokes every token issued
 * before it. Runs through the real security filter chain; every request uses its own client
 * address so tests do not share rate-limit buckets.
 */
@IntegrationTest
class PasswordChangeRequiredIntegrationTest {

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

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Test
    void accountRequiringPasswordChangeCanLogIn() throws Exception {
        String email = student(true);

        login(email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    void protectedApiIsForbiddenUntilPasswordIsChanged() throws Exception {
        String token = accessToken(student(true));

        mockMvc.perform(profile(token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("PASSWORD_CHANGE_REQUIRED"));
    }

    @Test
    void publicEndpointsStillAcceptRequestsCarryingThatToken() throws Exception {
        String email = student(true);
        String token = accessToken(email);

        mockMvc.perform(withToken(loginRequest(email, DevFixtureLoader.PASSWORD), token))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousPasswordChangeIsUnauthorized() throws Exception {
        String email = student(true);

        mockMvc.perform(changePassword(null, email, DevFixtureLoader.PASSWORD, newPassword()))
                .andExpect(status().isUnauthorized());

        assertThat(user(email).isPasswordChangeRequired()).isTrue();
    }

    @Test
    void cannotChangeAnotherAccountsPassword() throws Exception {
        String emailA = student(true);
        String emailB = student(true);
        String token = accessToken(emailA);
        String passwordA = user(emailA).getPassword();
        String passwordB = user(emailB).getPassword();
        int tokenVersionA = user(emailA).getTokenVersion();
        int tokenVersionB = user(emailB).getTokenVersion();

        mockMvc.perform(changePassword(token, emailB, DevFixtureLoader.PASSWORD, newPassword()))
                .andExpect(status().isForbidden());

        BaseUser a = user(emailA);
        BaseUser b = user(emailB);
        assertThat(b.getPassword()).isEqualTo(passwordB);
        assertThat(b.isPasswordChangeRequired()).isTrue();
        assertThat(b.getTokenVersion()).isEqualTo(tokenVersionB);
        assertThat(a.getPassword()).isEqualTo(passwordA);
        assertThat(a.isPasswordChangeRequired()).isTrue();
        assertThat(a.getTokenVersion()).isEqualTo(tokenVersionA);
    }

    /** Covers the forced first-login change and a later change from the profile. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void successfulChangeRevokesEarlierSessions(boolean passwordChangeRequired) throws Exception {
        String email = student(passwordChangeRequired);
        JsonNode oldTokens = loginTokens(email, DevFixtureLoader.PASSWORD);
        String oldAccess = oldTokens.path("accessToken").asText();
        String oldRefresh = oldTokens.path("refreshToken").asText();
        int tokenVersion = user(email).getTokenVersion();
        String newPassword = newPassword();

        mockMvc.perform(changePassword(oldAccess, email, DevFixtureLoader.PASSWORD, newPassword))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        BaseUser changed = user(email);
        assertThat(changed.isPasswordChangeRequired()).isFalse();
        assertThat(passwordEncoder.matches(newPassword, changed.getPassword())).isTrue();
        assertThat(changed.getTokenVersion()).isEqualTo(tokenVersion + 1);

        mockMvc.perform(profile(oldAccess)).andExpect(status().isUnauthorized());
        mockMvc.perform(refresh(oldRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));
        login(email, DevFixtureLoader.PASSWORD).andExpect(status().isUnauthorized());

        JsonNode newTokens = loginTokens(email, newPassword);
        assertThat(newTokens.path("passwordChangeRequired").asBoolean()).isFalse();
        mockMvc.perform(profile(newTokens.path("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mockMvc.perform(refresh(newTokens.path("refreshToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    void wrongCurrentPasswordKeepsEarlierSessions() throws Exception {
        String email = student(true);
        JsonNode tokens = loginTokens(email, DevFixtureLoader.PASSWORD);
        String access = tokens.path("accessToken").asText();
        String password = user(email).getPassword();
        int tokenVersion = user(email).getTokenVersion();

        mockMvc.perform(changePassword(access, email, "not-the-current-password", newPassword()))
                .andExpect(status().isBadRequest());

        BaseUser unchanged = user(email);
        assertThat(unchanged.getPassword()).isEqualTo(password);
        assertThat(unchanged.isPasswordChangeRequired()).isTrue();
        assertThat(unchanged.getTokenVersion()).isEqualTo(tokenVersion);
        mockMvc.perform(refresh(tokens.path("refreshToken").asText())).andExpect(status().isOk());
        mockMvc.perform(changePassword(access, email, DevFixtureLoader.PASSWORD, newPassword()))
                .andExpect(status().isOk());
    }

    @Test
    void invalidRequestKeepsEarlierSessions() throws Exception {
        String email = student(false);
        JsonNode tokens = loginTokens(email, DevFixtureLoader.PASSWORD);
        String access = tokens.path("accessToken").asText();
        int tokenVersion = user(email).getTokenVersion();

        mockMvc.perform(changePassword(access, email, DevFixtureLoader.PASSWORD, ""))
                .andExpect(status().isBadRequest());

        assertThat(user(email).getTokenVersion()).isEqualTo(tokenVersion);
        mockMvc.perform(profile(access)).andExpect(status().isOk());
        mockMvc.perform(refresh(tokens.path("refreshToken").asText())).andExpect(status().isOk());
    }

    @Test
    void accountWithoutPendingChangeUsesProtectedApis() throws Exception {
        String email = student(false);
        String token = accessToken(email);

        mockMvc.perform(profile(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void suspendedAccountCannotChangePasswordWithEarlierToken() throws Exception {
        String email = student(true);
        String token = accessToken(email);
        String password = user(email).getPassword();
        setStatus(email, Status.SUSPENDED);

        mockMvc.perform(changePassword(token, email, DevFixtureLoader.PASSWORD, newPassword()))
                .andExpect(status().isUnauthorized());

        BaseUser unchanged = user(email);
        assertThat(unchanged.getPassword()).isEqualTo(password);
        assertThat(unchanged.isPasswordChangeRequired()).isTrue();
    }

    private String student(boolean passwordChangeRequired) {
        String email = "password-change-" + UUID.randomUUID() + "@accounts.school.test";
        Student student = new Student();
        student.setRole(UserRole.STUDENT);
        student.setEmail(email);
        student.setFirstName("First");
        student.setLastName("Login");
        student.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        student.setStatus(Status.ACTIVE);
        student.setPasswordChangeRequired(passwordChangeRequired);
        student.setIsEmailVerified(true);
        studentRepository.saveAndFlush(student);
        return email;
    }

    private BaseUser user(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    private void setStatus(String email, Status status) {
        BaseUser user = user(email);
        user.setStatus(status);
        userRepository.saveAndFlush(user);
    }

    private static String newPassword() {
        return UUID.randomUUID().toString();
    }

    private String accessToken(String email) throws Exception {
        return loginTokens(email, DevFixtureLoader.PASSWORD).path("accessToken").asText();
    }

    /** Logs in and returns the login response data (access token, refresh token, user). */
    private JsonNode loginTokens(String email, String password) throws Exception {
        String body = login(email, password)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(body).path("data");
        assertThat(data.path("accessToken").isTextual()).isTrue();
        assertThat(data.path("refreshToken").isTextual()).isTrue();
        return data;
    }

    private ResultActions login(String email) throws Exception {
        return login(email, DevFixtureLoader.PASSWORD);
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(loginRequest(email, password));
    }

    private MockHttpServletRequestBuilder loginRequest(String email, String password) throws Exception {
        return withClientAddress(post("/api/auth/login"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password)));
    }

    private MockHttpServletRequestBuilder refresh(String refreshToken) throws Exception {
        return withClientAddress(post("/api/auth/refresh"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken)));
    }

    private MockHttpServletRequestBuilder changePassword(String token, String email, String oldPassword,
                                                         String newPassword) throws Exception {
        MockHttpServletRequestBuilder request = withClientAddress(post("/api/auth/change-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", email, "oldPassword", oldPassword, "newPassword", newPassword)));
        return withToken(request, token);
    }

    private static MockHttpServletRequestBuilder profile(String token) {
        return withToken(withClientAddress(get("/api/me/profile")), token);
    }

    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static MockHttpServletRequestBuilder withClientAddress(MockHttpServletRequestBuilder request) {
        return request.with(r -> {
            r.setRemoteAddr("10.33.0." + clientAddress.incrementAndGet());
            return r;
        });
    }
}
