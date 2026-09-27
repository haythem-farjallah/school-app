package com.example.school_management.feature.auth;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Account status enforcement for password login and for JWTs issued before a suspension,
 * through the real security filter chain. Every request uses its own client address so tests
 * do not share rate-limit buckets.
 */
@IntegrationTest
class SuspendedAccountAuthenticationIntegrationTest {

    private static final String PASSWORD = "Suspension-Pass-2024";

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
    void suspendedAccountCannotLogIn() throws Exception {
        String email = activeStudent();
        setStatus(email, Status.SUSPENDED);

        String body = login(email, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContainIgnoringCase("suspended").doesNotContainIgnoringCase("disabled");
    }

    @Test
    void suspendedAccountFailsLikeBadCredentials() throws Exception {
        String email = activeStudent();

        String wrongPassword = login(email, "not-the-password")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        setStatus(email, Status.SUSPENDED);
        String suspended = login(email, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(suspended).path("detail"))
                .isEqualTo(objectMapper.readTree(wrongPassword).path("detail"));
    }

    @Test
    void tokenIssuedBeforeSuspensionStopsWorking() throws Exception {
        String email = activeStudent();
        String token = accessToken(email);

        mockMvc.perform(profile(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        setStatus(email, Status.SUSPENDED);

        mockMvc.perform(profile(token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reactivatedAccountCanLogInAgain() throws Exception {
        String email = activeStudent();
        setStatus(email, Status.SUSPENDED);
        login(email, PASSWORD).andExpect(status().isUnauthorized());

        setStatus(email, Status.ACTIVE);

        String token = accessToken(email);
        mockMvc.perform(profile(token))
                .andExpect(status().isOk());
    }

    private String activeStudent() {
        String email = "suspension-" + UUID.randomUUID() + "@accounts.school.test";
        Student student = new Student();
        student.setRole(UserRole.STUDENT);
        student.setEmail(email);
        student.setFirstName("Sus");
        student.setLastName("Pended");
        student.setPassword(passwordEncoder.encode(PASSWORD));
        student.setStatus(Status.ACTIVE);
        student.setPasswordChangeRequired(false);
        student.setIsEmailVerified(true);
        studentRepository.saveAndFlush(student);
        return email;
    }

    private void setStatus(String email, Status status) {
        BaseUser user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(status);
        userRepository.saveAndFlush(user);
    }

    private String accessToken(String email) throws Exception {
        String body = login(email, PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return token.asText();
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(withClientAddress(post("/api/auth/login"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private static MockHttpServletRequestBuilder profile(String token) {
        return withClientAddress(get("/api/me/profile")).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static MockHttpServletRequestBuilder withClientAddress(MockHttpServletRequestBuilder request) {
        return request.with(r -> {
            r.setRemoteAddr("10.32.0." + clientAddress.incrementAndGet());
            return r;
        });
    }
}
