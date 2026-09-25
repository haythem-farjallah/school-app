package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Timetable requests that name a record which does not exist answer 404
 * through the real filter chain.
 */
@IntegrationTest
class TimetableNotFoundIntegrationTest {

    private static final long MISSING_ID = 999_999;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Test
    void missingTimetableIs404() throws Exception {
        String uri = "/api/v1/timetables/" + MISSING_ID;
        expectNotFound(mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, adminBearer())),
                "Timetable not found with id: " + MISSING_ID, uri);
    }

    @Test
    void slotWithMissingPeriodIs404() throws Exception {
        String uri = "/api/v1/timetables/slots";
        expectNotFound(mockMvc.perform(post(uri)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dayOfWeek", "MONDAY", "periodId", MISSING_ID)))),
                "Period not found with id: " + MISSING_ID, uri);
    }

    @Test
    void optimizingAMissingClassIs404() throws Exception {
        String uri = "/api/v1/timetables/class/" + MISSING_ID + "/optimize";
        expectNotFound(mockMvc.perform(post(uri).header(HttpHeaders.AUTHORIZATION, adminBearer())),
                "Class not found with id: " + MISSING_ID, uri);
    }

    private void expectNotFound(ResultActions result, String detail, String instance) throws Exception {
        result.andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value(instance))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    private String adminBearer() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.5." + clientAddress.incrementAndGet());
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
