package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.repository.LearningResourceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rejected learning-resource uploads answer 400 through the real filter chain and
 * store nothing. No test here uploads a valid file, so nothing is written to disk.
 */
@IntegrationTest
class LearningResourceUploadIntegrationTest {

    private static final String UPLOAD_URI = "/api/v1/learning-resources/upload";
    private static final byte[] PDF = "%PDF-1.4\n%test\n".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    LearningResourceRepository resources;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Test
    void emptyFileIs400() throws Exception {
        expectRejected(upload(new MockMultipartFile("file", "notes.pdf", "application/pdf", new byte[0])),
                "File cannot be empty");
    }

    @Test
    void malformedClassIdsIs400() throws Exception {
        expectRejected(upload(pdf()).param("classIds", "[1, two]"),
                "classIds: must be a JSON array of ids");
    }

    @Test
    void malformedCourseIdsIs400() throws Exception {
        expectRejected(upload(pdf()).param("courseIds", "{\"id\": 3}"),
                "courseIds: must be a JSON array of ids");
    }

    /** JSON null, null elements and values Jackson would coerce into a Long are not ids. */
    @ParameterizedTest
    @ValueSource(strings = {"null", "[1,null]", "[\"1\"]", "[1.5]", "[1] trailing", "7"})
    void classIdsThatAreNotAnArrayOfIntegersAre400(String classIds) throws Exception {
        expectRejected(upload(pdf()).param("classIds", classIds),
                "classIds: must be a JSON array of ids");
    }

    @Test
    void courseIdsWithANullIdIs400() throws Exception {
        expectRejected(upload(pdf()).param("courseIds", "[1,null]"),
                "courseIds: must be a JSON array of ids");
    }

    /** A valid id array passes parsing; the request then stops at file validation, so nothing is written. */
    @Test
    void validIdArraysReachFileValidation() throws Exception {
        expectRejected(upload(notAPdf()).param("classIds", "[1, 2]").param("courseIds", "[3]"),
                "File validation failed: File claims to be PDF but content doesn't match");
    }

    /** Blank and empty id lists are accepted, so this request reaches file validation. */
    @Test
    void fileRejectedBySecurityValidationIs400() throws Exception {
        expectRejected(upload(notAPdf()).param("classIds", " ").param("courseIds", "[]"),
                "File validation failed: File claims to be PDF but content doesn't match");
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "notes.pdf", "application/pdf", PDF);
    }

    private MockMultipartFile notAPdf() {
        return new MockMultipartFile("file", "notes.pdf", "application/pdf",
                "plain text pretending to be a PDF".getBytes(StandardCharsets.US_ASCII));
    }

    private MockHttpServletRequestBuilder upload(MockMultipartFile file) throws Exception {
        String bearer = teacherBearer();
        return multipart(UPLOAD_URI).file(file)
                .with(r -> {
                    r.setRemoteAddr("10.0.8." + clientAddress.incrementAndGet());
                    return r;
                })
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .param("title", "Chapter notes")
                .param("description", "Notes for chapter 1")
                .param("type", "DOCUMENT");
    }

    private void expectRejected(MockHttpServletRequestBuilder request, String detail) throws Exception {
        long before = resources.count();

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value(UPLOAD_URI))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(resources.count()).isEqualTo(before);
    }

    private String teacherBearer() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(r -> {
                            r.setRemoteAddr("10.0.8." + clientAddress.incrementAndGet());
                            return r;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", DevFixtureLoader.TEACHER_EMAIL, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return "Bearer " + token.asText();
    }
}
