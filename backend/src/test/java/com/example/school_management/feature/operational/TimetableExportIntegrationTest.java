package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.operational.entity.Timetable;
import com.example.school_management.feature.operational.repository.TimetableRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Timetable export and export preview through the real filter chain. Missing
 * timetables are covered by TimetableNotFoundIntegrationTest.
 */
@IntegrationTest
class TimetableExportIntegrationTest {

    private static final String UNSUPPORTED_FORMAT = "format: Format must be PDF, EXCEL, or CSV";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TimetableRepository timetables;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private Timetable timetable;

    @BeforeEach
    void createTimetable() {
        Timetable t = new Timetable();
        t.setName("Export test timetable");
        t.setAcademicYear("2025-2026");
        t.setSemester("Fall");
        timetable = timetables.save(t);
    }

    @AfterEach
    void deleteTimetable() {
        timetables.deleteById(timetable.getId());
    }

    @ParameterizedTest
    @CsvSource({
            "PDF, application/pdf",
            "EXCEL, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "CSV, text/csv"
    })
    void supportedFormatIsExported(String format, String contentType) throws Exception {
        export(format)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(contentType))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        startsWith("attachment; filename=\"timetable_" + timetable.getId() + "_")));
    }

    @Test
    void unsupportedFormatIs400() throws Exception {
        expectBadRequest(export("XML"), "/api/v1/timetables/" + timetable.getId() + "/export");
    }

    @Test
    void previewOfASupportedFormatSucceeds() throws Exception {
        preview("CSV")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timetableName").value("Export test timetable"))
                .andExpect(jsonPath("$.data.format").value("CSV"));
    }

    @Test
    void previewOfAnUnsupportedFormatIs400() throws Exception {
        expectBadRequest(preview("XML"), "/api/v1/timetables/" + timetable.getId() + "/export/preview");
    }

    private ResultActions export(String format) throws Exception {
        return mockMvc.perform(post("/api/v1/timetables/" + timetable.getId() + "/export")
                .header(HttpHeaders.AUTHORIZATION, adminBearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("format", format))));
    }

    private ResultActions preview(String format) throws Exception {
        return mockMvc.perform(get("/api/v1/timetables/" + timetable.getId() + "/export/preview")
                .param("format", format)
                .header(HttpHeaders.AUTHORIZATION, adminBearer()));
    }

    private void expectBadRequest(ResultActions result, String instance) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(UNSUPPORTED_FORMAT))
                .andExpect(jsonPath("$.instance").value(instance));
    }

    private String adminBearer() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.9." + clientAddress.incrementAndGet());
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
