package com.example.school_management.feature.auth;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.service.EmailService;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Forgot-password / reset-password through the real security filter chain and PostgreSQL. The
 * email boundary is mocked so the emailed code can be captured. Every request uses its own
 * client address so tests do not share rate-limit buckets.
 */
@IntegrationTest
class PasswordResetIntegrationTest {

    private static final String INVALID_OTP = "Invalid or expired OTP";

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

    @MockitoBean
    EmailService emailService;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    /* ------------------------------------------------------------ forgot-password */

    @Test
    void forgotPasswordAnswersActiveUnknownSuspendedAndDeletedAccountsAlike() throws Exception {
        String active = student(Status.ACTIVE);
        String suspended = student(Status.SUSPENDED);
        String deleted = student(Status.ACTIVE);
        setStatus(deleted, Status.DELETED);
        String unknown = "unknown-" + UUID.randomUUID() + "@accounts.school.test";

        MockHttpServletResponse activeResponse = forgot(active);
        List<MockHttpServletResponse> others = List.of(forgot(unknown), forgot(suspended), forgot(deleted));

        assertThat(activeResponse.getStatus()).isEqualTo(200);
        for (MockHttpServletResponse other : others) {
            assertThat(other.getStatus()).isEqualTo(200);
            assertThat(other.getContentAsString()).isEqualTo(activeResponse.getContentAsString());
            assertThat(other.getContentType()).isEqualTo(activeResponse.getContentType());
        }
        verify(emailService, times(1)).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
        verify(emailService).sendTemplateEmail(eq(active), anyString(), eq("otp"), anyMap());
        assertThat(user(active).getOtpCode()).isNotNull();
        assertThat(user(suspended).getOtpCode()).isNull();
    }

    @Test
    void codeIsStoredOnlyAsHash() throws Exception {
        String email = student(Status.ACTIVE);

        String code = requestCode(email);

        BaseUser user = user(email);
        assertThat(code).matches("[0-9]{6}");
        assertThat(user.getOtpCode()).isNotEqualTo(code).doesNotContain(code);
        assertThat(passwordEncoder.matches(code, user.getOtpCode())).isTrue();
        assertThat(user.getOtpFailedAttempts()).isZero();
        assertThat(user.getOtpExpiry()).isCloseTo(LocalDateTime.now().plusMinutes(10), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void resendWithinCooldownKeepsTheCurrentCodeAndLaterReplacesIt() throws Exception {
        String email = student(Status.ACTIVE);
        String first = requestCode(email);
        String firstHash = user(email).getOtpCode();

        assertThat(forgot(email).getStatus()).isEqualTo(200);

        verify(emailService, times(1)).sendTemplateEmail(eq(email), anyString(), eq("otp"), anyMap());
        assertThat(user(email).getOtpCode()).isEqualTo(firstHash);

        // Move the issue time past the cooldown and leave some failed attempts behind.
        BaseUser user = user(email);
        user.setOtpExpiry(user.getOtpExpiry().minusSeconds(61));
        user.setOtpFailedAttempts(3);
        userRepository.saveAndFlush(user);

        String second = requestCode(email, 2);

        BaseUser replaced = user(email);
        assertThat(replaced.getOtpCode()).isNotEqualTo(firstHash);
        assertThat(replaced.getOtpFailedAttempts()).isZero();
        assertThat(passwordEncoder.matches(second, replaced.getOtpCode())).isTrue();
        if (!first.equals(second)) {
            expectInvalidOtp(reset(email, first, newPassword()));
        }
        mockMvc.perform(reset(email, second, newPassword())).andExpect(status().isOk());
    }

    @Test
    void failedEmailLeavesNoCodeAndStillAnswers200() throws Exception {
        String email = student(Status.ACTIVE);
        doThrow(new MailSendException("SMTP unavailable"))
                .when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());

        MockHttpServletResponse response = forgot(email);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEmpty();
        BaseUser user = user(email);
        assertThat(user.getOtpCode()).isNull();
        assertThat(user.getOtpExpiry()).isNull();
    }

    /* ------------------------------------------------------------ reset-password */

    @Test
    void correctCodeResetsPasswordOnce() throws Exception {
        String email = student(Status.ACTIVE);
        String code = requestCode(email);
        String newPassword = newPassword();

        mockMvc.perform(reset(email, code, newPassword)).andExpect(status().isOk());

        BaseUser user = user(email);
        assertThat(passwordEncoder.matches(newPassword, user.getPassword())).isTrue();
        assertThat(user.isPasswordChangeRequired()).isFalse();
        assertThat(user.getOtpCode()).isNull();
        assertThat(user.getOtpExpiry()).isNull();
        assertThat(user.getOtpFailedAttempts()).isZero();
        assertThat(login(email, newPassword)).isEqualTo(200);
        assertThat(login(email, DevFixtureLoader.PASSWORD)).isEqualTo(401);

        expectInvalidOtp(reset(email, code, newPassword()));
        assertThat(passwordEncoder.matches(newPassword, user(email).getPassword())).isTrue();
    }

    @Test
    void successfulResetRevokesEarlierSessions() throws Exception {
        String email = student(Status.ACTIVE);
        JsonNode oldTokens = loginTokens(email, DevFixtureLoader.PASSWORD);
        String oldAccess = oldTokens.path("accessToken").asText();
        String oldRefresh = oldTokens.path("refreshToken").asText();
        int tokenVersion = user(email).getTokenVersion();
        String newPassword = newPassword();

        mockMvc.perform(reset(email, requestCode(email), newPassword)).andExpect(status().isOk());

        assertThat(user(email).getTokenVersion()).isEqualTo(tokenVersion + 1);
        mockMvc.perform(profile(oldAccess)).andExpect(status().isUnauthorized());
        mockMvc.perform(refresh(oldRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));
        assertThat(login(email, DevFixtureLoader.PASSWORD)).isEqualTo(401);

        JsonNode newTokens = loginTokens(email, newPassword);
        mockMvc.perform(profile(newTokens.path("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mockMvc.perform(refresh(newTokens.path("refreshToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());
        assertThat(user(email).getTokenVersion()).isEqualTo(tokenVersion + 1);
    }

    @Test
    void failedResetKeepsEarlierSessions() throws Exception {
        String email = student(Status.ACTIVE);
        JsonNode tokens = loginTokens(email, DevFixtureLoader.PASSWORD);
        int tokenVersion = user(email).getTokenVersion();
        String code = requestCode(email);

        expectInvalidOtp(reset(email, wrong(code), newPassword()));

        assertThat(user(email).getTokenVersion()).isEqualTo(tokenVersion);
        mockMvc.perform(profile(tokens.path("accessToken").asText())).andExpect(status().isOk());
        mockMvc.perform(refresh(tokens.path("refreshToken").asText())).andExpect(status().isOk());
    }

    @Test
    void resetClearsRequiredPasswordChange() throws Exception {
        String email = student(Status.ACTIVE);
        BaseUser user = user(email);
        user.setPasswordChangeRequired(true);
        userRepository.saveAndFlush(user);

        mockMvc.perform(reset(email, requestCode(email), newPassword())).andExpect(status().isOk());

        assertThat(user(email).isPasswordChangeRequired()).isFalse();
    }

    @Test
    void fiveWrongCodesDiscardTheCode() throws Exception {
        String email = student(Status.ACTIVE);
        String code = requestCode(email);
        String password = user(email).getPassword();

        for (int attempt = 1; attempt <= 4; attempt++) {
            expectInvalidOtp(reset(email, wrong(code), newPassword()));
            assertThat(user(email).getOtpFailedAttempts()).isEqualTo(attempt);
            assertThat(user(email).getOtpCode()).isNotNull();
        }
        expectInvalidOtp(reset(email, wrong(code), newPassword()));

        BaseUser locked = user(email);
        assertThat(locked.getOtpCode()).isNull();
        assertThat(locked.getOtpExpiry()).isNull();

        expectInvalidOtp(reset(email, code, newPassword()));
        assertThat(user(email).getPassword()).isEqualTo(password);
    }

    @Test
    void newCodeAfterLockoutStartsWithoutFailedAttemptsAndWorks() throws Exception {
        String email = student(Status.ACTIVE);
        String code = requestCode(email);
        for (int attempt = 1; attempt <= 5; attempt++) {
            expectInvalidOtp(reset(email, wrong(code), newPassword()));
        }

        String newCode = requestCode(email, 2);

        assertThat(user(email).getOtpFailedAttempts()).isZero();
        String newPassword = newPassword();
        mockMvc.perform(reset(email, newCode, newPassword)).andExpect(status().isOk());
        assertThat(passwordEncoder.matches(newPassword, user(email).getPassword())).isTrue();
    }

    @Test
    void concurrentWrongCodesAreAllCounted() throws Exception {
        String email = student(Status.ACTIVE);
        String code = requestCode(email);
        List<Callable<Integer>> attempts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            attempts.add(() -> mockMvc.perform(reset(email, wrong(code), newPassword())).andReturn().getResponse().getStatus());
        }

        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Integer> statuses = new ArrayList<>();
        try {
            for (Future<Integer> result : pool.invokeAll(attempts)) {
                statuses.add(result.get());
            }
        } finally {
            pool.shutdown();
        }

        assertThat(statuses).containsOnly(401);
        assertThat(user(email).getOtpCode()).isNull();
        expectInvalidOtp(reset(email, code, newPassword()));
    }

    @Test
    void expiredCodeIsRejectedAndDiscarded() throws Exception {
        String email = student(Status.ACTIVE);
        String code = requestCode(email);
        String password = user(email).getPassword();
        BaseUser user = user(email);
        user.setOtpExpiry(LocalDateTime.now().minusSeconds(1));
        userRepository.saveAndFlush(user);

        expectInvalidOtp(reset(email, code, newPassword()));

        BaseUser unchanged = user(email);
        assertThat(unchanged.getPassword()).isEqualTo(password);
        assertThat(unchanged.getOtpCode()).isNull();
        assertThat(unchanged.getOtpExpiry()).isNull();
    }

    @Test
    void resetAnswersWrongCodeUnknownAndSuspendedAccountsAlike() throws Exception {
        String active = student(Status.ACTIVE);
        String activeCode = requestCode(active);
        String suspended = student(Status.ACTIVE);
        String suspendedCode = requestCode(suspended);
        String suspendedPassword = user(suspended).getPassword();
        setStatus(suspended, Status.SUSPENDED);
        String unknown = "unknown-" + UUID.randomUUID() + "@accounts.school.test";

        String wrongCode = expectInvalidOtp(reset(active, wrong(activeCode), newPassword()));
        String unknownAccount = expectInvalidOtp(reset(unknown, "123456", newPassword()));
        String suspendedAccount = expectInvalidOtp(reset(suspended, suspendedCode, newPassword()));

        assertThat(unknownAccount).isEqualTo(wrongCode);
        assertThat(suspendedAccount).isEqualTo(wrongCode);
        assertThat(user(suspended).getPassword()).isEqualTo(suspendedPassword);
    }

    /* ------------------------------------------------------------ validation */

    @Test
    void malformedRequestsAreRejected() throws Exception {
        String email = student(Status.ACTIVE);

        mockMvc.perform(withClientAddress(post("/api/auth/forgot-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "not-an-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        expectBadRequest(reset("not-an-email", "123456", newPassword()));
        expectBadRequest(reset(email, "12345", newPassword()));
        expectBadRequest(reset(email, "1234567", newPassword()));
        expectBadRequest(reset(email, "12a456", newPassword()));
        expectBadRequest(reset(email, "123456", "x".repeat(7)));
        expectBadRequest(reset(email, "123456", "x".repeat(129)));

        verify(emailService, never()).sendTemplateEmail(anyString(), anyString(), anyString(), any());
    }

    /* ------------------------------------------------------------ helpers */

    private String requestCode(String email) throws Exception {
        return requestCode(email, 1);
    }

    /** Requests a code and returns the one emailed with the given (1-based) email to that address. */
    @SuppressWarnings("unchecked")
    private String requestCode(String email, int emailNumber) throws Exception {
        assertThat(forgot(email).getStatus()).isEqualTo(200);
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(emailService, times(emailNumber))
                .sendTemplateEmail(eq(email), anyString(), eq("otp"), variables.capture());
        return (String) variables.getValue().get("code");
    }

    private MockHttpServletResponse forgot(String email) throws Exception {
        return mockMvc.perform(withClientAddress(post("/api/auth/forgot-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andReturn().getResponse();
    }

    private MockHttpServletRequestBuilder reset(String email, String otp, String newPassword) throws Exception {
        return withClientAddress(post("/api/auth/reset-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", email, "otp", otp, "newPassword", newPassword)));
    }

    /** Asserts the generic 401 of a rejected reset and returns the raw body. */
    private String expectInvalidOtp(MockHttpServletRequestBuilder reset) throws Exception {
        return mockMvc.perform(reset)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value(INVALID_OTP))
                .andReturn().getResponse().getContentAsString();
    }

    private void expectBadRequest(MockHttpServletRequestBuilder reset) throws Exception {
        mockMvc.perform(reset)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private int login(String email, String password) throws Exception {
        return mockMvc.perform(withClientAddress(post("/api/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn().getResponse().getStatus();
    }

    /** Logs in and returns the login response data (access token, refresh token, user). */
    private JsonNode loginTokens(String email, String password) throws Exception {
        String body = mockMvc.perform(withClientAddress(post("/api/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data");
    }

    private MockHttpServletRequestBuilder refresh(String refreshToken) throws Exception {
        return withClientAddress(post("/api/auth/refresh"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken)));
    }

    private static MockHttpServletRequestBuilder profile(String accessToken) {
        return withClientAddress(get("/api/me/profile"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    /** A six-digit code that differs from the given one. */
    private static String wrong(String code) {
        return String.format("%06d", (Integer.parseInt(code) + 1) % 1_000_000);
    }

    private String student(Status status) {
        String email = "password-reset-" + UUID.randomUUID() + "@accounts.school.test";
        Student student = new Student();
        student.setRole(UserRole.STUDENT);
        student.setEmail(email);
        student.setFirstName("Reset");
        student.setLastName("Password");
        student.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        student.setStatus(status);
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

    private static MockHttpServletRequestBuilder withClientAddress(MockHttpServletRequestBuilder request) {
        int n = clientAddress.incrementAndGet();
        return request.with(r -> {
            r.setRemoteAddr("10.35." + (n / 250) + "." + (n % 250 + 1));
            return r;
        });
    }
}
