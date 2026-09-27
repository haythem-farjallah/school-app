package com.example.school_management.commons.filter;

import com.example.school_management.commons.configs.RateLimitingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter that applies different rate limits based on endpoint patterns.
 *
 * <p>Buckets live in this instance's memory, so limits are per application instance.
 * Running several backend replicas needs a shared or edge rate limiter instead.
 */
@Slf4j
@Component
public class RateLimitingFilter implements Filter {
    
    // In-memory storage for rate limiting buckets
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    // Exact immediate peer addresses allowed to supply X-Forwarded-For / X-Real-IP.
    private final Set<String> trustedProxies;

    public RateLimitingFilter(ObjectMapper objectMapper,
                              @Value("${app.security.trusted-proxies}") String[] trustedProxies) {
        this.objectMapper = objectMapper;
        this.trustedProxies = Set.copyOf(List.of(trustedProxies));
    }

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        String clientIp = getClientIpAddress(httpRequest);
        String normalizedUri = normalizeUri(requestURI);
        
        // Create a unique key for rate limiting (IP + endpoint pattern)
        String rateLimitKey = String.format("%s:%s:%s", clientIp, method, normalizedUri);
        
        // Get the appropriate bucket based on endpoint pattern
        Bucket bucket = getBucketForEndpoint(requestURI, method, rateLimitKey);
        
        // Try to consume a token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Request is allowed
            httpResponse.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            httpResponse.setHeader("X-Rate-Limit-Reset", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            chain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for {} {}", method, normalizedUri);
            
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;

            // Runs before any controller, so GlobalExceptionHandler never sees this rejection.
            ProblemDetail body = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.");
            body.setInstance(URI.create(requestURI));
            body.setProperty("retryAfter", retryAfterSeconds);

            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            httpResponse.setHeader("X-Rate-Limit-Remaining", "0");
            httpResponse.setHeader("X-Rate-Limit-Reset", String.valueOf(retryAfterSeconds));
            objectMapper.writeValue(httpResponse.getOutputStream(), body);
        }
    }

    /**
     * Get the appropriate bucket based on endpoint pattern
     */
    private Bucket getBucketForEndpoint(String requestURI, String method, String rateLimitKey) {
        // Authentication endpoints
        if (requestURI.startsWith("/api/auth/")) {
            return buckets.computeIfAbsent(rateLimitKey, k -> RateLimitingConfig.createAuthBucket());
        }
        
        // Admin endpoints
        if (requestURI.startsWith("/api/v1/admins/") || 
            requestURI.contains("/admin") ||
            (requestURI.startsWith("/api/v1/") && method.equals("DELETE"))) {
            return buckets.computeIfAbsent(rateLimitKey, k -> RateLimitingConfig.createAdminBucket());
        }
        
        // Upload endpoints
        if (requestURI.contains("/upload") || 
            requestURI.contains("/export") ||
            requestURI.contains("/import") ||
            requestURI.contains("/file")) {
            return buckets.computeIfAbsent(rateLimitKey, k -> RateLimitingConfig.createUploadBucket());
        }
        
        // Listing endpoints (GET requests to list resources)
        if (method.equals("GET") && (
            requestURI.startsWith("/api/v1/students") ||
            requestURI.startsWith("/api/v1/teachers") ||
            requestURI.startsWith("/api/v1/classes") ||
            requestURI.startsWith("/api/v1/courses") ||
            requestURI.startsWith("/api/v1/timetables") ||
            requestURI.startsWith("/api/v1/announcements") ||
            requestURI.startsWith("/api/v1/resources") ||
            requestURI.startsWith("/api/v1/grades") ||
            requestURI.startsWith("/api/v1/dashboard"))) {
            return buckets.computeIfAbsent(rateLimitKey, k -> RateLimitingConfig.createListingBucket());
        }
        
        // Default API bucket for all other endpoints
        return buckets.computeIfAbsent(rateLimitKey, k -> RateLimitingConfig.createApiBucket());
    }

    /**
     * Normalize URI to group similar endpoints (e.g., /api/v1/students/123 -> /api/v1/students/{id})
     */
    private String normalizeUri(String uri) {
        // Replace numeric IDs with placeholder
        return uri.replaceAll("/\\d+", "/{id}")
                  .replaceAll("/\\d+/", "/{id}/");
    }

    /**
     * Client address used as the rate-limit identity.
     *
     * <p>Forwarding headers are honoured only when the immediate peer is a configured trusted proxy;
     * otherwise any client could pick a fresh bucket per request. X-Forwarded-For is read right to left
     * and trusted hops are skipped, so the first untrusted hop is the address a trusted proxy saw
     * rather than a value the client supplied at the left of the chain.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxies.contains(remoteAddr)) {
            return remoteAddr;
        }

        String forwardedFor = String.join(",", Collections.list(request.getHeaders("X-Forwarded-For")));
        String[] hops = forwardedFor.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (!hop.isEmpty() && !trustedProxies.contains(hop)) {
                return hop;
            }
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return remoteAddr;
    }
}
