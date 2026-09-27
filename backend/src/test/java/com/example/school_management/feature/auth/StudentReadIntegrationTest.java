package com.example.school_management.feature.auth;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reading students repeatedly, and after changing them, against the real PostgreSQL and Redis:
 * every read answers from the current data. Each test works on its own student.
 */
@IntegrationTest
class StudentReadIntegrationTest {

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String bearer;

    @BeforeEach
    void signIn() throws Exception {
        bearer = adminBearer();
    }

    @Test
    void theListAndADetailCanBeReadRepeatedly() throws Exception {
        long id = createStudent("Rita");

        for (int read = 0; read < 3; read++) {
            list().andExpect(status().isOk()).andExpect(jsonPath("$.data.content[*].id").value(hasItem((int) id)));
            detail(id).andExpect(status().isOk()).andExpect(jsonPath("$.data.firstName").value("Rita"));
        }
    }

    @Test
    void theCreatedProfileIncludingTheBirthdayIsReadBack() throws Exception {
        long id = createStudent("Lina");

        detail(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.birthday").value("2011-05-09"))
                .andExpect(jsonPath("$.data.gender").value("F"))
                .andExpect(jsonPath("$.data.gradeLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.enrollmentYear").value(2024));
    }

    @Test
    void anUpdateIsVisibleOnTheNextRead() throws Exception {
        long id = createStudent("Omar");
        list().andExpect(status().isOk());
        detail(id).andExpect(status().isOk()).andExpect(jsonPath("$.data.firstName").value("Omar"));

        mockMvc.perform(patch("/api/v1/students/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Omari", "lastName", "Reader", "gradeLevel", "UNIVERSITY", "enrollmentYear", 2025))))
                .andExpect(status().isOk());

        detail(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Omari"))
                .andExpect(jsonPath("$.data.birthday").value("2011-05-09"))
                .andExpect(jsonPath("$.data.gradeLevel").value("UNIVERSITY"))
                .andExpect(jsonPath("$.data.enrollmentYear").value(2025));
        list().andExpect(status().isOk()).andExpect(jsonPath("$.data.content[*].firstName").value(hasItem("Omari")));
    }

    @Test
    void aDeletedStudentLeavesTheList() throws Exception {
        long id = createStudent("Dana");
        list().andExpect(status().isOk()).andExpect(jsonPath("$.data.content[*].id").value(hasItem((int) id)));
        detail(id).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/students/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());

        list().andExpect(status().isOk()).andExpect(jsonPath("$.data.content[*].id").value(not(hasItem((int) id))));
        detail(id).andExpect(status().isNotFound());
    }

    private long createStudent(String firstName) throws Exception {
        Map<String, Object> profile = Map.of(
                "firstName", firstName,
                "lastName", "Reader",
                "email", "student-" + UUID.randomUUID() + "@fixtures.school.test",
                "birthday", "2011-05-09",
                "gender", "F");
        String body = mockMvc.perform(post("/api/v1/students")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "profile", profile, "gradeLevel", "HIGH", "enrollmentYear", 2024))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    private ResultActions list() throws Exception {
        return mockMvc.perform(get("/api/v1/students").param("page", "0").param("size", "1000")
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions detail(long id) throws Exception {
        return mockMvc.perform(get("/api/v1/students/" + id).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private String adminBearer() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.11." + clientAddress.incrementAndGet());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", DevFixtureLoader.ADMIN_EMAIL, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return "Bearer " + token.asText();
    }
}
