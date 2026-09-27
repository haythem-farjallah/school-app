package com.example.school_management.commons.configs;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import java.time.Duration;

/**
 * Bucket4j limits per endpoint category. RateLimitingFilter keeps the buckets in local memory,
 * so rate-limit state belongs to one application instance; multiple backend replicas need a
 * shared or edge rate limiter later.
 */
public class RateLimitingConfig {

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
