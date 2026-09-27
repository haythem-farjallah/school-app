package com.example.school_management.commons.configs;

import com.example.school_management.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP CORS and the SockJS endpoint both follow app.security.allowed-origins, here configured as a
 * comma-separated list with surrounding spaces.
 */
@IntegrationTest
@TestPropertySource(properties = "app.security.allowed-origins= http://localhost:5173 , http://localhost:4173 ")
class CorsIntegrationTest {

    private static final String[] ALLOWED = {"http://localhost:5173", "http://localhost:4173"};
    private static final String UNKNOWN = "http://evil.example";

    @Autowired
    MockMvc mockMvc;

    @Test
    void apiPreflightFromConfiguredOriginsIsAccepted() throws Exception {
        for (String origin : ALLOWED) {
            mockMvc.perform(preflight("/api/auth/login", origin))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        }
    }

    @Test
    void apiPreflightFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(preflight("/api/auth/login", UNKNOWN))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void apiRequestFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(get("/api/me/profile").header(HttpHeaders.ORIGIN, UNKNOWN))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void sockJsFollowsTheSameOrigins() throws Exception {
        for (String origin : ALLOWED) {
            mockMvc.perform(get("/ws/info").header(HttpHeaders.ORIGIN, origin))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
        }
        mockMvc.perform(get("/ws/info").header(HttpHeaders.ORIGIN, UNKNOWN))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private static MockHttpServletRequestBuilder preflight(String uri, String origin) {
        return options(uri)
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Authorization");
    }
}
