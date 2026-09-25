package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import com.example.school_management.feature.academic.repository.LearningResourceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ownership checks on learning resources answer 403 with the reason, through the
 * real filter chain. The resource has no creator, so the fixture teacher does not
 * own it.
 */
@IntegrationTest
class LearningResourceOwnershipIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    LearningResourceRepository resourceRepository;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private LearningResource resource;

    @BeforeEach
    void createResourceWithoutCreator() {
        LearningResource r = new LearningResource();
        r.setTitle("Ownership test resource");
        r.setUrl("https://example.test/resource.pdf");
        r.setType(ResourceType.DOCUMENT);
        resource = resourceRepository.save(r);
    }

    @AfterEach
    void deleteResource() {
        resourceRepository.findById(resource.getId()).ifPresent(resourceRepository::delete);
    }

    @Test
    void teacherCannotUpdateAResourceTheyDidNotCreate() throws Exception {
        String uri = "/api/v1/learning-resources/" + resource.getId();
        expectForbidden(mockMvc.perform(put(uri)
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed\"}")),
                "You can only update resources you created", uri);

        assertThat(resourceRepository.findById(resource.getId()).orElseThrow().getTitle())
                .isEqualTo("Ownership test resource");
    }

    @Test
    void teacherCannotDeleteAResourceTheyDidNotCreate() throws Exception {
        String uri = "/api/v1/learning-resources/" + resource.getId();
        expectForbidden(mockMvc.perform(delete(uri).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL))),
                "You can only delete resources you created", uri);

        assertThat(resourceRepository.existsById(resource.getId())).isTrue();
    }

    private void expectForbidden(ResultActions result, String detail, String instance) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value(instance))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.6." + clientAddress.incrementAndGet());
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
