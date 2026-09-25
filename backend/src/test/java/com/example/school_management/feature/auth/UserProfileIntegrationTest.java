package com.example.school_management.feature.auth;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/me/profile through the real Spring Security filter chain.
 * Each test works on its own fixture account so tests do not depend on each other's writes.
 */
@IntegrationTest
class UserProfileIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Test
    void profileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/me/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telephone\":\"+216 20 000 000\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReturnsTheSignedInUsersOwnContactDetails() throws Exception {
        BaseUser student = userRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();
        student.setTelephone("+216 71 000 111");
        student.setAddress("12 Rue de Marseille, Tunis");
        userRepository.save(student);

        mockMvc.perform(get("/api/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.STUDENT_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId()))
                .andExpect(jsonPath("$.email").value(DevFixtureLoader.STUDENT_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Sam"))
                .andExpect(jsonPath("$.lastName").value("Student"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.telephone").value("+216 71 000 111"))
                .andExpect(jsonPath("$.address").value("12 Rue de Marseille, Tunis"))
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void patchSavesContactDetailsAndReturnsTheUpdatedProfile() throws Exception {
        String authorization = bearer(DevFixtureLoader.TEACHER_EMAIL);
        String update = objectMapper.writeValueAsString(Map.of(
                "telephone", "+216 98 765 432",
                "address", "5 Avenue Habib Bourguiba, Sousse",
                "firstName", "Changed",
                "email", "changed@fixtures.school.test"));

        mockMvc.perform(patch("/api/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telephone").value("+216 98 765 432"))
                .andExpect(jsonPath("$.address").value("5 Avenue Habib Bourguiba, Sousse"))
                .andExpect(jsonPath("$.firstName").value("Theo"))
                .andExpect(jsonPath("$.email").value(DevFixtureLoader.TEACHER_EMAIL));

        mockMvc.perform(get("/api/me/profile").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telephone").value("+216 98 765 432"))
                .andExpect(jsonPath("$.address").value("5 Avenue Habib Bourguiba, Sousse"))
                .andExpect(jsonPath("$.firstName").value("Theo"))
                .andExpect(jsonPath("$.email").value(DevFixtureLoader.TEACHER_EMAIL));

        BaseUser teacher = userRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        assertThat(teacher.getTelephone()).isEqualTo("+216 98 765 432");
        assertThat(teacher.getAddress()).isEqualTo("5 Avenue Habib Bourguiba, Sousse");
        assertThat(teacher.getFirstName()).isEqualTo("Theo");
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(login(email))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return "Bearer " + token.asText();
    }

    private RequestBuilder login(String email) throws Exception {
        return post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr("10.0.1." + clientAddress.incrementAndGet());
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", DevFixtureLoader.PASSWORD)));
    }
}
