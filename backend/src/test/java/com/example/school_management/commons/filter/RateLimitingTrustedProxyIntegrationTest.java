package com.example.school_management.commons.filter;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.configs.RateLimitingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rate-limit identity behind configured proxies, through the real security filter chain.
 * The property is a comma-separated list with surrounding spaces.
 */
@IntegrationTest
@TestPropertySource(properties = "app.security.trusted-proxies= 10.0.9.1 , 10.0.9.2 ")
class RateLimitingTrustedProxyIntegrationTest {

    private static final String LOGIN_URI = "/api/auth/login";
    private static final String EDGE_PROXY = "10.0.9.1";
    private static final String INNER_PROXY = "10.0.9.2";
    private static final String UNTRUSTED_ADDRESS = "10.0.4.1";

    private static final long CAPACITY = RateLimitingConfig.RateLimits.AUTH_BANDWIDTH.getCapacity();

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void forwardedClientsBehindTrustedProxyGetSeparateBuckets() throws Exception {
        mockMvc.perform(failedLogin(EDGE_PROXY).header("X-Forwarded-For", "198.51.100.10"))
                .andExpect(remaining(CAPACITY - 1));
        mockMvc.perform(failedLogin(EDGE_PROXY).header("X-Forwarded-For", "198.51.100.11"))
                .andExpect(remaining(CAPACITY - 1));
        mockMvc.perform(failedLogin(EDGE_PROXY).header("X-Forwarded-For", "198.51.100.10"))
                .andExpect(remaining(CAPACITY - 2));
    }

    @Test
    void spoofedLeftmostForwardedForCannotResetBucket() throws Exception {
        // The trusted edge appends the real source; whatever the client put before it is ignored.
        for (int i = 0; i < CAPACITY; i++) {
            mockMvc.perform(failedLogin(EDGE_PROXY).header("X-Forwarded-For", "fake-" + i + ", 198.51.100.50"))
                    .andExpect(remaining(CAPACITY - 1 - i));
        }

        mockMvc.perform(failedLogin(EDGE_PROXY).header("X-Forwarded-For", "fake-last, 198.51.100.50"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void trustedHopsAreSkippedFromRightToLeft() throws Exception {
        // Inner proxy received from the edge proxy, which received from the client.
        mockMvc.perform(failedLogin(INNER_PROXY).header("X-Forwarded-For", "fake, 203.0.113.50, " + EDGE_PROXY))
                .andExpect(remaining(CAPACITY - 1));
        // The same client reaching the edge proxy directly shares that bucket.
        mockMvc.perform(failedLogin(EDGE_PROXY).header("X-Forwarded-For", "203.0.113.50"))
                .andExpect(remaining(CAPACITY - 2));
    }

    @Test
    void realIpIsHonouredOnlyFromTrustedProxy() throws Exception {
        mockMvc.perform(failedLogin(EDGE_PROXY).header("X-Real-IP", "198.51.100.60"))
                .andExpect(remaining(CAPACITY - 1));
        mockMvc.perform(failedLogin(EDGE_PROXY).header("X-Real-IP", "198.51.100.61"))
                .andExpect(remaining(CAPACITY - 1));

        // An untrusted sender naming the same client keeps its own bucket.
        mockMvc.perform(failedLogin(UNTRUSTED_ADDRESS).header("X-Real-IP", "198.51.100.60"))
                .andExpect(remaining(CAPACITY - 1));
        mockMvc.perform(failedLogin(UNTRUSTED_ADDRESS))
                .andExpect(remaining(CAPACITY - 2));
    }

    private static ResultMatcher remaining(long tokens) {
        return header().string("X-Rate-Limit-Remaining", String.valueOf(tokens));
    }

    private MockHttpServletRequestBuilder failedLogin(String remoteAddress) throws Exception {
        return post(LOGIN_URI)
                .with(request -> {
                    request.setRemoteAddr(remoteAddress);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", "rate-limit@example.test", "password", "not-the-password")));
    }
}
