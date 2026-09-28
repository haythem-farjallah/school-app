package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The current-user dashboard is open to every signed-in account; the base dashboard
 * by id is self-only except for administrators.
 */
@IntegrationTest
class DashboardSelfAccessIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Test
    void currentUserDashboardIsOpenToSignedInAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/current-user")
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.PARENT_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("PARENT"));

        mockMvc.perform(get("/api/v1/dashboard/current-user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void baseDashboardIsSelfOnlyExceptForAdmins() throws Exception {
        long studentId = idOf(DevFixtureLoader.STUDENT_EMAIL);
        long teacherId = idOf(DevFixtureLoader.TEACHER_EMAIL);
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);

        mockMvc.perform(get("/api/v1/dashboard/base/{id}", studentId).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userInfo.id").value(studentId));

        mockMvc.perform(get("/api/v1/dashboard/base/{id}", teacherId).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/dashboard/base/{id}", studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/dashboard/base/{id}", teacherId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userInfo.id").value(teacherId));
    }

    private long idOf(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.22." + clientAddress.incrementAndGet());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return "Bearer " + token.asText();
    }
}
