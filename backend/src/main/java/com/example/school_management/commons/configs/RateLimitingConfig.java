package com.example.school_management.commons.configs;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.resource.ClientResources;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;

/**
 * Bucket4j limits per endpoint category, and the Redis-backed proxy manager that holds bucket state.
 *
 * <p>Bucket state is stored in Redis and shared by all backend instances. The Redis deployment may
 * still be a single instance; high availability is infrastructure work, not bucket semantics.
 */
@Configuration
public class RateLimitingConfig {

    // Keys expire this long after the bucket would have refilled completely.
    private static final Duration KEEP_AFTER_REFILL = Duration.ofMinutes(1);

    // Authentication endpoints - stricter limits
    public static final BucketConfiguration AUTH_CONFIGURATION = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(10).refillIntervally(10, Duration.ofMinutes(1)).build())
            .build();

    // General API endpoints - moderate limits
    public static final BucketConfiguration API_CONFIGURATION = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(100).refillIntervally(100, Duration.ofMinutes(1)).build())
            .build();

    // Listing endpoints - higher limits
    public static final BucketConfiguration LISTING_CONFIGURATION = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(200).refillIntervally(200, Duration.ofMinutes(1)).build())
            .build();

    // File upload endpoints - lower limits
    public static final BucketConfiguration UPLOAD_CONFIGURATION = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(20).refillIntervally(20, Duration.ofMinutes(1)).build())
            .build();

    // Admin endpoints - moderate limits
    public static final BucketConfiguration ADMIN_CONFIGURATION = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder().capacity(50).refillIntervally(50, Duration.ofMinutes(1)).build())
            .build();

    /**
     * Native Lettuce client for Bucket4j, pointed at the same Redis as Spring Data Redis
     * (spring.data.redis.* or a service connection) and sharing its event loops.
     */
    @Bean(destroyMethod = "shutdown")
    RedisClient rateLimitRedisClient(ClientResources clientResources,
                                     RedisConnectionDetails connectionDetails,
                                     RedisProperties properties) {
        RedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
        RedisURI.Builder uri = RedisURI.builder()
                .withHost(standalone.getHost())
                .withPort(standalone.getPort())
                .withDatabase(standalone.getDatabase());
        if (connectionDetails.getPassword() != null) {
            if (connectionDetails.getUsername() != null) {
                uri.withAuthentication(connectionDetails.getUsername(), connectionDetails.getPassword());
            } else {
                uri.withPassword(connectionDetails.getPassword().toCharArray());
            }
        }
        if (properties.getTimeout() != null) {
            uri.withTimeout(properties.getTimeout());
        }

        RedisClient client = RedisClient.create(clientResources, uri.build());
        if (properties.getConnectTimeout() != null) {
            client.setOptions(ClientOptions.builder()
                    .socketOptions(SocketOptions.builder().connectTimeout(properties.getConnectTimeout()).build())
                    .build());
        }
        return client;
    }

    /**
     * Lazy so that Redis being down at startup does not stop the application; the filter then
     * answers 503 until a connection can be made. The connection is thread-safe and closed with the client.
     */
    @Bean
    @Lazy
    ProxyManager<byte[]> rateLimitBuckets(RedisClient rateLimitRedisClient) {
        return Bucket4jLettuce.casBasedBuilder(rateLimitRedisClient.connect(ByteArrayCodec.INSTANCE))
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(KEEP_AFTER_REFILL))
                .build();
    }
}
