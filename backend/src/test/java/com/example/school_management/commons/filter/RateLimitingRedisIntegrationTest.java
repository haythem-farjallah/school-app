package com.example.school_management.commons.filter;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.configs.RateLimitingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Rate-limit state lives in the Testcontainers Redis, not in the application.
 */
@IntegrationTest
class RateLimitingRedisIntegrationTest {

    private static final String LOGIN_URI = "/api/auth/login";

    // No other test uses these addresses, so each auth bucket starts full.
    private static final String EXPIRY_CLIENT_ADDRESS = "10.0.12.1";
    private static final String SHARED_STATE_CLIENT_ADDRESS = "10.0.12.2";

    private static final long CAPACITY = RateLimitingConfig.AUTH_CONFIGURATION.getBandwidths()[0].getCapacity();

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    RedisConnectionDetails redisConnectionDetails;

    @Test
    void bucketIsStoredUnderHashedNamespacedKeyThatExpires() throws Exception {
        mockMvc.perform(failedLogin(EXPIRY_CLIENT_ADDRESS)).andExpect(remaining(CAPACITY - 1));

        String key = new String(RateLimitingFilter.redisKey(EXPIRY_CLIENT_ADDRESS + ":POST:" + LOGIN_URI),
                StandardCharsets.UTF_8);
        assertThat(key).matches("school-app:rate-limit:v1:[0-9a-f]{64}");
        assertThat(redis.keys("school-app:rate-limit:v1:*")).contains(key);
        assertThat(redis.keys("*" + EXPIRY_CLIENT_ADDRESS + "*")).isEmpty();

        // -1 would mean no expiry. The auth bucket refills within a minute, plus one minute kept afterwards.
        assertThat(redis.getExpire(key, TimeUnit.MILLISECONDS)).isBetween(1L, Duration.ofMinutes(2).toMillis());
    }

    @Test
    void freshProxyManagerSharesStateWithTheFilter() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(failedLogin(SHARED_STATE_CLIENT_ADDRESS)).andExpect(remaining(CAPACITY - 1 - i));
        }

        // A separate client and proxy manager, as another backend instance would have.
        byte[] key = RateLimitingFilter.redisKey(SHARED_STATE_CLIENT_ADDRESS + ":POST:" + LOGIN_URI);
        RedisConnectionDetails.Standalone server = redisConnectionDetails.getStandalone();
        RedisClient client = RedisClient.create(RedisURI.create(server.getHost(), server.getPort()));
        try (StatefulRedisConnection<byte[], byte[]> connection = client.connect(ByteArrayCodec.INSTANCE)) {
            ProxyManager<byte[]> freshBuckets = Bucket4jLettuce.casBasedBuilder(connection)
                    .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                            Duration.ofMinutes(1)))
                    .build();
            BucketProxy bucket = freshBuckets.builder().build(key, RateLimitingConfig.AUTH_CONFIGURATION);

            assertThat(bucket.getAvailableTokens()).isEqualTo(CAPACITY - 3);
            assertThat(bucket.tryConsume(1)).isTrue();
        } finally {
            client.shutdown();
        }

        mockMvc.perform(failedLogin(SHARED_STATE_CLIENT_ADDRESS)).andExpect(remaining(CAPACITY - 5));
    }

    private static ResultMatcher remaining(long tokens) {
        return header().string("X-Rate-Limit-Remaining", String.valueOf(tokens));
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
