package com.example.school_management.feature.auth;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login through the real Spring Security filter chain (JWT, rate limiting, method security).
 * Each login uses its own client address so tests do not share rate-limit buckets.
 */
@IntegrationTest
class AuthLoginIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @ParameterizedTest
    @CsvSource({
            DevFixtureLoader.ADMIN_EMAIL + ", ADMIN",
            DevFixtureLoader.TEACHER_EMAIL + ", TEACHER",
            DevFixtureLoader.STUDENT_EMAIL + ", STUDENT",
            DevFixtureLoader.PARENT_EMAIL + ", PARENT"
    })
    void fixtureAccountsCanLogIn(String email, String role) throws Exception {
        mockMvc.perform(login(email, DevFixtureLoader.PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.role").value(role))
                .andExpect(jsonPath("$.data.passwordChangeRequired").value(false));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(login(DevFixtureLoader.ADMIN_EMAIL, "not-the-password"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRequiresAValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/courses").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAuthenticatesRequestsAndRolesAreEnforced() throws Exception {
        String adminToken = accessToken(DevFixtureLoader.ADMIN_EMAIL);
        mockMvc.perform(get("/api/v1/courses").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        String parentToken = accessToken(DevFixtureLoader.PARENT_EMAIL);
        mockMvc.perform(get("/api/v1/courses").header(HttpHeaders.AUTHORIZATION, "Bearer " + parentToken))
                .andExpect(status().isForbidden());
    }

    private String accessToken(String email) throws Exception {
        String body = mockMvc.perform(login(email, DevFixtureLoader.PASSWORD))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return token.asText();
    }

    private RequestBuilder login(String email, String password) throws Exception {
        return post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr("10.0.0." + clientAddress.incrementAndGet());
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password)));
    }
}
