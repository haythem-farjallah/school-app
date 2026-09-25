package com.example.school_management.commons.exceptions;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The problem-detail body written by GlobalExceptionHandler, through the real
 * security filter chain. A test-only controller throws each handled exception.
 */
@IntegrationTest
@Import(GlobalExceptionHandlerIntegrationTest.ThrowingController.class)
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Test
    void validationErrorIs400WithTheFieldMessages() throws Exception {
        String uri = "/api/test/errors/validation";
        expectProblem(mockMvc.perform(post(uri)
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}")),
                HttpStatus.BAD_REQUEST, "name: must not be blank", uri);
    }

    @Test
    void resourceNotFoundIs404WithTheExceptionMessage() throws Exception {
        String uri = "/api/test/errors/not-found";
        expectProblem(mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))),
                HttpStatus.NOT_FOUND, "Student 42 not found", uri);
    }

    @Test
    void conflictIs409WithTheExceptionMessage() throws Exception {
        String uri = "/api/test/errors/conflict";
        expectProblem(mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))),
                HttpStatus.CONFLICT, "Student is already enrolled in this class", uri);
    }

    @Test
    void malformedJsonOnARealEndpointIs400() throws Exception {
        String uri = "/api/me/profile";
        expectProblem(mockMvc.perform(patch(uri)
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.STUDENT_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telephone\":")),
                HttpStatus.BAD_REQUEST, "Malformed JSON request", uri);
    }

    @Test
    void duplicateEmailConstraintIs409WithoutSqlDetails() throws Exception {
        String uri = "/api/test/errors/duplicate-email";
        String body = expectProblem(mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))),
                HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", uri);

        assertThat(body).doesNotContain("users_email_key", "duplicate key", "someone@school.test");
    }

    @Test
    void responseStatusExceptionKeepsItsStatusAndReason() throws Exception {
        String uri = "/api/test/errors/response-status";
        expectProblem(mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))),
                HttpStatus.UNPROCESSABLE_ENTITY, "Grade must be between 0 and 20", uri);
    }

    @Test
    void methodSecurityDenialIs403() throws Exception {
        String uri = "/api/test/errors/admin-only";
        expectProblem(mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.STUDENT_EMAIL))),
                HttpStatus.FORBIDDEN, "ACCESS_DENIED", uri);
    }

    @Test
    void unexpectedExceptionIs500WithoutInternalDetails() throws Exception {
        String uri = "/api/test/errors/unexpected";
        String body = expectProblem(mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))),
                HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR", uri);

        assertThat(body).doesNotContain("IllegalStateException", "select password", "ThrowingController", "at com.");
    }

    /** Asserts the problem-detail contract and returns the raw body. */
    private String expectProblem(ResultActions result, HttpStatus status, String detail, String instance) throws Exception {
        String body = result
                .andExpect(status().is(status.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(status.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(status.value()))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value(instance))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.statusCode").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(body).size()).isEqualTo(5);
        return body;
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
                    request.setRemoteAddr("10.0.2." + clientAddress.incrementAndGet());
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", DevFixtureLoader.PASSWORD)));
    }

    public record NamedRequest(@NotBlank String name) {
    }

    @RestController
    @RequestMapping("/api/test/errors")
    public static class ThrowingController {

        @PostMapping("/validation")
        public void validation(@Valid @RequestBody NamedRequest request) {
        }

        @GetMapping("/not-found")
        public void notFound() {
            throw new ResourceNotFoundException("Student 42 not found");
        }

        @GetMapping("/conflict")
        public void conflict() {
            throw new ConflictException("Student is already enrolled in this class");
        }

        @GetMapping("/duplicate-email")
        public void duplicateEmail() {
            throw new DataIntegrityViolationException("could not execute statement", new SQLException(
                    "ERROR: duplicate key value violates unique constraint \"users_email_key\" "
                            + "Detail: Key (email)=(someone@school.test) already exists."));
        }

        @GetMapping("/response-status")
        public void responseStatus() {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Grade must be between 0 and 20");
        }

        @GetMapping("/admin-only")
        @PreAuthorize("hasRole('ADMIN')")
        public void adminOnly() {
        }

        @GetMapping("/unexpected")
        public void unexpected() {
            throw new IllegalStateException("select password from users where id = 1");
        }
    }
}
