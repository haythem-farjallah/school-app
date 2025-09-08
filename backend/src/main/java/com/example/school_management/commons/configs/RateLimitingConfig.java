package com.example.school_management.commons.configs;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate limiting configuration using Bucket4j and Redis
 */
@Slf4j
@Configuration
public class RateLimitingConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * Redis client for rate limiting
     */
    @Bean
    public RedisClient redisClient() {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort)
                .withDatabase(redisDatabase);

        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            builder.withPassword(redisPassword.toCharArray());
        }

        return RedisClient.create(builder.build());
    }

    /**
     * Redis client for rate limiting operations
     */
    @Bean
    public RedisClient rateLimitingRedisClient() {
        return redisClient();
    }

    /**
     * Rate limiting configurations for different endpoint types
     */
    public static class RateLimits {
        
        // Authentication endpoints - stricter limits
        public static final Bandwidth AUTH_BANDWIDTH = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
        
        // General API endpoints - moderate limits
        public static final Bandwidth API_BANDWIDTH = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        
        // Listing endpoints - higher limits
        public static final Bandwidth LISTING_BANDWIDTH = Bandwidth.classic(200, Refill.intervally(200, Duration.ofMinutes(1)));
        
        // File upload endpoints - lower limits
        public static final Bandwidth UPLOAD_BANDWIDTH = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
        
        // Admin endpoints - moderate limits
        public static final Bandwidth ADMIN_BANDWIDTH = Bandwidth.classic(50, Refill.intervally(50, Duration.ofMinutes(1)));
    }

    /**
     * Create a bucket for authentication endpoints
     */
    public static Bucket createAuthBucket() {
        return Bucket.builder()
                .addLimit(RateLimits.AUTH_BANDWIDTH)
                .build();
    }

    /**
     * Create a bucket for general API endpoints
     */
    public static Bucket createApiBucket() {
        return Bucket.builder()
                .addLimit(RateLimits.API_BANDWIDTH)
                .build();
    }

    /**
     * Create a bucket for listing endpoints
     */
    public static Bucket createListingBucket() {
        return Bucket.builder()
                .addLimit(RateLimits.LISTING_BANDWIDTH)
                .build();
    }

    /**
     * Create a bucket for upload endpoints
     */
    public static Bucket createUploadBucket() {
        return Bucket.builder()
                .addLimit(RateLimits.UPLOAD_BANDWIDTH)
                .build();
    }

    /**
     * Create a bucket for admin endpoints
     */
    public static Bucket createAdminBucket() {
        return Bucket.builder()
                .addLimit(RateLimits.ADMIN_BANDWIDTH)
                .build();
    }
}
