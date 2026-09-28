package com.example.school_management.commons.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.lettuce.core.RedisConnectionException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The boundary between rate-limiter failures (503) and failures further down the filter chain.
 * Redis is mocked here only to simulate an outage; the Redis-backed behaviour is covered by
 * RateLimitingRedisIntegrationTest.
 */
class RateLimitingFilterTest {

    private static final String CLIENT_ADDRESS = "198.51.100.25";

    // Spring's builder registers the ProblemDetail mix-in, as the application ObjectMapper does.
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final BucketProxy bucket = mock(BucketProxy.class);
    private final FilterChain chain = mock(FilterChain.class);
    private RateLimitingFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ProxyManager<byte[]> buckets = mock(ProxyManager.class);
        RemoteBucketBuilder<byte[]> builder = mock(RemoteBucketBuilder.class);
        when(buckets.builder()).thenReturn(builder);
        when(builder.build(any(byte[].class), any(BucketConfiguration.class))).thenReturn(bucket);
        filter = new RateLimitingFilter(objectMapper, buckets, new String[0]);
    }

    @Test
    void redisFailureFailsClosedWithGenericServiceUnavailable() throws Exception {
        when(bucket.tryConsumeAndReturnRemaining(1))
                .thenThrow(new RedisConnectionException("Unable to connect to redis.internal:6379"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(loginRequest(), response, chain);

        verifyNoInteractions(chain);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getHeader("X-Rate-Limit-Remaining")).isNull();

        String body = response.getContentAsString();
        assertThat(objectMapper.readTree(body).get("detail").asText())
                .isEqualTo("Rate limiting service temporarily unavailable.");
        assertThat(objectMapper.readTree(body).size()).isEqualTo(5);
        assertThat(body).doesNotContain("redis.internal", "6379", "RedisConnectionException", CLIENT_ADDRESS);
    }

    @Test
    void downstreamFailureIsNotTreatedAsRateLimiterFailure() throws Exception {
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.consumed(9, 0));
        IllegalStateException downstreamFailure = new IllegalStateException("controller failed");
        doThrow(downstreamFailure).when(chain).doFilter(any(), any());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(loginRequest(), response, chain)).isSameAs(downstreamFailure);

        verify(chain, times(1)).doFilter(any(), any());
        assertThat(response.getStatus()).isNotEqualTo(503);
        assertThat(response.getContentAsString()).isEmpty();
    }

    private static MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(CLIENT_ADDRESS);
        return request;
    }
}
