package com.example.school_management.commons.filter;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.configs.RateLimitingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 429 responses written by RateLimitingFilter, through the real security filter chain.
 */
@IntegrationTest
class RateLimitingFilterIntegrationTest {

    private static final String LOGIN_URI = "/api/auth/login";

    // No other test uses this address, so the auth bucket starts full.
    private static final String CLIENT_ADDRESS = "10.0.3.1";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void exhaustedBucketGetsProblemDetailWithRetryAfter() throws Exception {
        // One request more than the bucket holds is always rejected.
        long capacity = RateLimitingConfig.RateLimits.AUTH_BANDWIDTH.getCapacity();
        for (int i = 0; i < capacity; i++) {
            mockMvc.perform(failedLogin());
        }

        String body = mockMvc.perform(failedLogin())
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("X-Rate-Limit-Remaining", "0"))
                .andExpect(header().string("X-Rate-Limit-Reset", matchesPattern("\\d+")))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.detail").value("Too many requests. Please try again later."))
                .andExpect(jsonPath("$.instance").value(LOGIN_URI))
                .andExpect(jsonPath("$.retryAfter").isNumber())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.statusCode").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(body).size()).isEqualTo(6);
        assertThat(body).doesNotContain(CLIENT_ADDRESS);
    }

    private RequestBuilder failedLogin() throws Exception {
        return post(LOGIN_URI)
                .with(request -> {
                    request.setRemoteAddr(CLIENT_ADDRESS);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", "rate-limit@example.test", "password", "not-the-password")));
    }
}
