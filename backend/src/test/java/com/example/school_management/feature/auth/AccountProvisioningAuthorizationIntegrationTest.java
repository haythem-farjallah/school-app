package com.example.school_management.feature.auth;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Account provisioning (/api/auth/register and /api/admin/**) through the real security filter chain.
 * Every request uses its own client address so tests do not share rate-limit buckets.
 */
@IntegrationTest
class AccountProvisioningAuthorizationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Test
    void anonymousCallerCannotRegisterAccounts() throws Exception {
        String email = uniqueEmail("anon-admin");

        mockMvc.perform(register(email, "ADMIN"))
                .andExpect(status().isUnauthorized());

        assertThat(userRepository.existsByEmail(email)).isFalse();
    }

    @Test
    void studentCannotRegisterAccounts() throws Exception {
        String email = uniqueEmail("student-made-admin");

        mockMvc.perform(register(email, "ADMIN").header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.STUDENT_EMAIL)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.existsByEmail(email)).isFalse();
    }

    @Test
    void adminCanRegisterAccounts() throws Exception {
        String email = uniqueEmail("registered-teacher");

        mockMvc.perform(register(email, "TEACHER").header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL)))
                .andExpect(status().isCreated());

        assertThat(userRepository.existsByEmail(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {DevFixtureLoader.STUDENT_EMAIL, DevFixtureLoader.TEACHER_EMAIL, DevFixtureLoader.PARENT_EMAIL})
    void nonAdminCannotCreateAdministrators(String callerEmail) throws Exception {
        String email = uniqueEmail("escalated-admin");

        mockMvc.perform(createAdministrator(email).header(HttpHeaders.AUTHORIZATION, bearer(callerEmail)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.existsByEmail(email)).isFalse();
    }

    @Test
    void adminCanCreateAdministrators() throws Exception {
        String email = uniqueEmail("created-admin");

        mockMvc.perform(createAdministrator(email).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL)))
                .andExpect(status().isCreated());

        assertThat(userRepository.existsByEmail(email)).isTrue();
    }

    @Test
    void staffKeepsPeopleDirectoriesButCannotCreateAdministrators() throws Exception {
        String staffEmail = uniqueEmail("staff");
        mockMvc.perform(register(staffEmail, "STAFF").header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL)))
                .andExpect(status().isCreated());
        completeFirstLogin(staffEmail);
        String staffToken = bearer(staffEmail);

        mockMvc.perform(withClientAddress(get("/api/admin/teachers")).header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk());

        String adminEmail = uniqueEmail("staff-made-admin");
        mockMvc.perform(createAdministrator(adminEmail).header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isForbidden());
        assertThat(userRepository.existsByEmail(adminEmail)).isFalse();
    }

    // Provisioned accounts must change their password before using the API; this test is about STAFF authorization.
    private void completeFirstLogin(String email) {
        BaseUser user = userRepository.findByEmail(email).orElseThrow();
        user.setPasswordChangeRequired(false);
        userRepository.saveAndFlush(user);
    }

    private MockHttpServletRequestBuilder register(String email, String role) throws Exception {
        return withClientAddress(post("/api/auth/register"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "firstName", "Provisioned",
                        "lastName", "Account",
                        "email", email,
                        "password", DevFixtureLoader.PASSWORD,
                        "role", role)));
    }

    private MockHttpServletRequestBuilder createAdministrator(String email) throws Exception {
        return withClientAddress(post("/api/admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "profile", Map.of(
                                "firstName", "Provisioned",
                                "lastName", "Administrator",
                                "email", email,
                                "role", "ADMIN"),
                        "password", DevFixtureLoader.PASSWORD)));
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(withClientAddress(post("/api/auth/login"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return "Bearer " + token.asText();
    }

    private static MockHttpServletRequestBuilder withClientAddress(MockHttpServletRequestBuilder request) {
        return request.with(r -> {
            r.setRemoteAddr("10.31.0." + clientAddress.incrementAndGet());
            return r;
        });
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@provisioning.school.test";
    }
}
