package com.example.school_management.commons.filter;

import com.example.school_management.commons.configs.RateLimitingConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter that applies different rate limits based on endpoint patterns
 */
@Slf4j
@Component
public class RateLimitingFilter implements Filter {
    
    // In-memory storage for rate limiting buckets
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        String clientIp = getClientIpAddress(httpRequest);
        
        // Create a unique key for rate limiting (IP + endpoint pattern)
        String rateLimitKey = createRateLimitKey(clientIp, requestURI, method);
        
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
            log.warn("Rate limit exceeded for IP: {} on endpoint: {} {}", clientIp, method, requestURI);
            
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setHeader("X-Rate-Limit-Remaining", "0");
            httpResponse.setHeader("X-Rate-Limit-Reset", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            
            // Simple JSON response without ObjectMapper to avoid circular dependency
            String errorResponse = String.format(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\",\"retryAfter\":%d}",
                probe.getNanosToWaitForRefill() / 1_000_000_000
            );
            
            httpResponse.getWriter().write(errorResponse);
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
     * Create a rate limit key based on client IP and endpoint
     */
    private String createRateLimitKey(String clientIp, String requestURI, String method) {
        // Normalize the URI to group similar endpoints
        String normalizedUri = normalizeUri(requestURI);
        return String.format("%s:%s:%s", clientIp, method, normalizedUri);
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
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}