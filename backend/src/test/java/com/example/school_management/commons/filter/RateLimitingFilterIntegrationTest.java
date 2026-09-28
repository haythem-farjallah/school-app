package com.example.school_management.commons.filter;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.configs.RateLimitingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bucket selection and 429 responses of RateLimitingFilter, through the real security filter chain.
 * No trusted proxies are configured here (the default), so forwarding headers must be ignored.
 */
@IntegrationTest
class RateLimitingFilterIntegrationTest {

    private static final String LOGIN_URI = "/api/auth/login";
    private static final String REFRESH_URI = "/api/auth/refresh";

    // No other test uses these addresses, so each auth bucket starts full.
    private static final String CLIENT_ADDRESS = "10.0.3.1";
    private static final String SINGLE_REQUEST_CLIENT_ADDRESS = "10.0.3.2";
    private static final String FORWARDED_FOR_SPOOFING_ADDRESS = "10.0.3.3";
    private static final String REAL_IP_SPOOFING_ADDRESS = "10.0.3.4";
    private static final String REFRESH_ADDRESS = "10.0.3.5";
    private static final String SHARED_ADDRESS = "10.0.3.6";
    private static final String API_ADDRESS = "10.0.3.7";

    private static final long AUTH_CAPACITY = RateLimitingConfig.AUTH_CONFIGURATION.getBandwidths()[0].getCapacity();
    private static final long REFRESH_CAPACITY = RateLimitingConfig.REFRESH_CONFIGURATION.getBandwidths()[0].getCapacity();
    private static final long API_CAPACITY = RateLimitingConfig.API_CONFIGURATION.getBandwidths()[0].getCapacity();

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void eachRequestConsumesExactlyOneToken() throws Exception {
        // The filter runs inside the security chain only; a second servlet registration would consume twice.
        long capacity = RateLimitingConfig.AUTH_CONFIGURATION.getBandwidths()[0].getCapacity();

        mockMvc.perform(failedLogin(SINGLE_REQUEST_CLIENT_ADDRESS))
                .andExpect(header().string("X-Rate-Limit-Remaining", String.valueOf(capacity - 1)));
    }

    @Test
    void exhaustedBucketGetsProblemDetailWithRetryAfter() throws Exception {
        // One request more than the bucket holds is always rejected.
        long capacity = RateLimitingConfig.AUTH_CONFIGURATION.getBandwidths()[0].getCapacity();
        for (int i = 0; i < capacity; i++) {
            mockMvc.perform(failedLogin(CLIENT_ADDRESS));
        }

        String body = mockMvc.perform(failedLogin(CLIENT_ADDRESS))
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

    @Test
    void untrustedClientCannotSpoofForwardedForIntoFreshBuckets() throws Exception {
        long capacity = RateLimitingConfig.AUTH_CONFIGURATION.getBandwidths()[0].getCapacity();
        for (int i = 0; i < capacity; i++) {
            mockMvc.perform(failedLogin(FORWARDED_FOR_SPOOFING_ADDRESS).header("X-Forwarded-For", "198.51.100." + i))
                    .andExpect(header().string("X-Rate-Limit-Remaining", String.valueOf(capacity - 1 - i)));
        }

        mockMvc.perform(failedLogin(FORWARDED_FOR_SPOOFING_ADDRESS).header("X-Forwarded-For", "198.51.100.200"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void untrustedClientCannotSpoofRealIpIntoFreshBuckets() throws Exception {
        long capacity = RateLimitingConfig.AUTH_CONFIGURATION.getBandwidths()[0].getCapacity();
        for (int i = 0; i < capacity; i++) {
            mockMvc.perform(failedLogin(REAL_IP_SPOOFING_ADDRESS).header("X-Real-IP", "198.51.100." + i))
                    .andExpect(header().string("X-Rate-Limit-Remaining", String.valueOf(capacity - 1 - i)));
        }

        mockMvc.perform(failedLogin(REAL_IP_SPOOFING_ADDRESS).header("X-Real-IP", "198.51.100.200"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void refreshUsesItsOwnLargerBucket() throws Exception {
        assertThat(REFRESH_CAPACITY).isGreaterThan(AUTH_CAPACITY);

        mockMvc.perform(failedRefresh(REFRESH_ADDRESS))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Rate-Limit-Remaining", String.valueOf(REFRESH_CAPACITY - 1)));
    }

    @Test
    void refreshAndLoginFromOneAddressDoNotDrainEachOther() throws Exception {
        // More refreshes than the login bucket holds, all still answered by the endpoint.
        for (int i = 0; i < AUTH_CAPACITY + 5; i++) {
            mockMvc.perform(failedRefresh(SHARED_ADDRESS)).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(failedLogin(SHARED_ADDRESS))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Rate-Limit-Remaining", String.valueOf(AUTH_CAPACITY - 1)));

        // An exhausted login bucket leaves refresh available.
        for (int i = 1; i < AUTH_CAPACITY; i++) {
            mockMvc.perform(failedLogin(SHARED_ADDRESS));
        }
        mockMvc.perform(failedLogin(SHARED_ADDRESS)).andExpect(status().isTooManyRequests());
        mockMvc.perform(failedRefresh(SHARED_ADDRESS))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Rate-Limit-Remaining", String.valueOf(REFRESH_CAPACITY - AUTH_CAPACITY - 6)));
    }

    @Test
    void otherAuthenticationEndpointsKeepTheStrictBucket() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .with(remoteAddress(REFRESH_ADDRESS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(header().string("X-Rate-Limit-Remaining", String.valueOf(AUTH_CAPACITY - 1)));
    }

    @Test
    void generalApiRequestsKeepTheApiBucket() throws Exception {
        mockMvc.perform(get("/api/me/profile").with(remoteAddress(API_ADDRESS)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Rate-Limit-Remaining", String.valueOf(API_CAPACITY - 1)));
    }

    private MockHttpServletRequestBuilder failedRefresh(String clientAddress) throws Exception {
        return post(REFRESH_URI)
                .with(remoteAddress(clientAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", "not-a-refresh-token")));
    }

    private static RequestPostProcessor remoteAddress(String clientAddress) {
        return request -> {
            request.setRemoteAddr(clientAddress);
            return request;
        };
    }

    private MockHttpServletRequestBuilder failedLogin(String clientAddress) throws Exception {
        return post(LOGIN_URI)
                .with(request -> {
                    request.setRemoteAddr(clientAddress);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("email", "rate-limit@example.test", "password", "not-the-password")));
    }
}
