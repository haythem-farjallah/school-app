package com.example.school_management.commons.filter;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * X-Request-ID correlation through the real filter chain. A test-only controller reports the
 * request ID it sees in the MDC, so the tests compare it with the response header.
 */
@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
@Import(RequestIdFilterIntegrationTest.RequestIdProbeController.class)
class RequestIdFilterIntegrationTest {

    private static final String PROBE_URI = "/api/test/request-id";
    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void requestWithoutAnIdGetsAGeneratedOne() throws Exception {
        MockHttpServletResponse response = probe(get(PROBE_URI));

        String requestId = response.getHeader(RequestIdFilter.HEADER);
        assertThat(requestId).matches(UUID_PATTERN);
        assertThat(response.getContentAsString()).isEqualTo(requestId);
    }

    @Test
    void validIncomingIdIsKept() throws Exception {
        MockHttpServletResponse response = probe(get(PROBE_URI).header(RequestIdFilter.HEADER, "client-42_trace.A"));

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("client-42_trace.A");
        assertThat(response.getContentAsString()).isEqualTo("client-42_trace.A");
    }

    @Test
    void incomingIdOfTheMaximumLengthIsKept() throws Exception {
        String longest = "a".repeat(64);
        MockHttpServletResponse response = probe(get(PROBE_URI).header(RequestIdFilter.HEADER, longest));

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(longest);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "abc\ndef",
            "abc\r\n2026-01-01 INFO forged log line",
            "tab\there",
            "has space",
            "requestId=forged",
            "<script>",
            "semi;colon",
            "ünïcode",
    })
    void unsafeIncomingIdIsReplaced(String unsafe) throws Exception {
        MockHttpServletResponse response = probe(get(PROBE_URI).header(RequestIdFilter.HEADER, unsafe));

        String requestId = response.getHeader(RequestIdFilter.HEADER);
        assertThat(requestId).matches(UUID_PATTERN);
        assertThat(response.getContentAsString()).isEqualTo(requestId);
    }

    @Test
    void overlyLongIncomingIdIsReplaced() throws Exception {
        String tooLong = "a".repeat(65);
        MockHttpServletResponse response = probe(get(PROBE_URI).header(RequestIdFilter.HEADER, tooLong));

        assertThat(response.getHeader(RequestIdFilter.HEADER)).matches(UUID_PATTERN);
    }

    @Test
    void mdcIsClearedWhenTheRequestCompletes() throws Exception {
        probe(get(PROBE_URI).header(RequestIdFilter.HEADER, "cleared-after"));

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void consecutiveRequestsDoNotShareAnId() throws Exception {
        MockHttpServletResponse first = probe(get(PROBE_URI).header(RequestIdFilter.HEADER, "first-request"));
        MockHttpServletResponse second = probe(get(PROBE_URI));

        assertThat(first.getContentAsString()).isEqualTo("first-request");
        String secondId = second.getHeader(RequestIdFilter.HEADER);
        assertThat(secondId).isNotEqualTo("first-request").matches(UUID_PATTERN);
        assertThat(second.getContentAsString()).isEqualTo(secondId);
    }

    @Test
    void applicationLogLinesCarryTheRequestId(CapturedOutput output) throws Exception {
        probe(get(PROBE_URI).header(RequestIdFilter.HEADER, "log-line-check"));

        assertThat(output.getOut().lines())
                .anySatisfy(line -> assertThat(line).contains("requestId=log-line-check", "request-id probe"));
    }

    @Test
    void unauthenticatedResponseCarriesTheRequestId() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/me/profile")
                        .header(RequestIdFilter.HEADER, "unauthenticated-call"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse();

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("unauthenticated-call");
    }

    @Test
    void forbiddenResponseCarriesTheRequestId() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/admin/staff")
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.STUDENT_EMAIL))
                        .header(RequestIdFilter.HEADER, "forbidden-call"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse();

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("forbidden-call");
    }

    @Test
    void unexpectedErrorResponseCarriesTheRequestId() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get(PROBE_URI + "/failure")
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))
                        .header(RequestIdFilter.HEADER, "failing-call"))
                .andExpect(status().isInternalServerError())
                .andReturn().getResponse();

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("failing-call");
    }

    private MockHttpServletResponse probe(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            // A client address of its own, so logins do not share a rate-limit bucket.
                            request.setRemoteAddr("10.0.3." + clientAddress.incrementAndGet());
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

    @Slf4j
    @RestController
    @RequestMapping(PROBE_URI)
    public static class RequestIdProbeController {

        @GetMapping
        public String requestId() {
            log.info("request-id probe");
            return MDC.get(RequestIdFilter.MDC_KEY);
        }

        @GetMapping("/failure")
        public String failure() {
            throw new IllegalStateException("probe failure");
        }
    }
}
