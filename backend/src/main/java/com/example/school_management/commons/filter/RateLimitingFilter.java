package com.example.school_management.commons.filter;

import com.example.school_management.commons.configs.RateLimitingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * Rate limiting filter that applies different rate limits based on endpoint patterns.
 *
 * <p>Buckets live in Redis, so every backend instance shares the same limits. If Redis cannot be
 * reached the request is refused with 503 rather than passed through unlimited.
 */
@Slf4j
@Component
public class RateLimitingFilter implements Filter {

    static final String REDIS_KEY_PREFIX = "school-app:rate-limit:v1:";

    // Percent-decodes the path and drops ;parameters, as Spring MVC does before matching a handler.
    private static final UrlPathHelper PATH_HELPER = UrlPathHelper.defaultInstance;

    private final ObjectMapper objectMapper;

    private final ProxyManager<byte[]> buckets;

    // Exact immediate peer addresses allowed to supply X-Forwarded-For / X-Real-IP.
    private final Set<String> trustedProxies;

    public RateLimitingFilter(ObjectMapper objectMapper,
                              @Lazy ProxyManager<byte[]> buckets,
                              @Value("${app.security.trusted-proxies}") String[] trustedProxies) {
        this.objectMapper = objectMapper;
        this.buckets = buckets;
        this.trustedProxies = Set.copyOf(List.of(trustedProxies));
    }

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        // Classify and key by the decoded path Spring MVC routes on, not the raw URI: otherwise
        // /api/auth/%6Cogin reaches the login endpoint with a fresh bucket of its own.
        String path = PATH_HELPER.getPathWithinApplication(httpRequest);
        String method = httpRequest.getMethod();
        String clientIp = getClientIpAddress(httpRequest);
        String normalizedUri = normalizeUri(path);
        
        // Create a unique key for rate limiting (IP + endpoint pattern)
        String rateLimitKey = String.format("%s:%s:%s", clientIp, method, normalizedUri);
        BucketConfiguration configuration = getConfigurationForEndpoint(path, method);

        // Only the Redis round trip is guarded; failures further down the chain must propagate unchanged.
        ConsumptionProbe probe;
        try {
            probe = buckets.builder()
                    .build(redisKey(rateLimitKey), configuration)
                    .tryConsumeAndReturnRemaining(1);
        } catch (RuntimeException e) {
            log.error("Rate limiter unavailable for {} {}: {}", method, normalizedUri, e.getClass().getName());
            writeProblem(httpResponse, problem(HttpStatus.SERVICE_UNAVAILABLE,
                    "Rate limiting service temporarily unavailable.", requestURI));
            return;
        }

        if (!probe.isConsumed()) {
            log.warn("Rate limit exceeded for {} {}", method, normalizedUri);
            
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            httpResponse.setHeader("X-Rate-Limit-Remaining", "0");
            httpResponse.setHeader("X-Rate-Limit-Reset", String.valueOf(retryAfterSeconds));
            ProblemDetail body = problem(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Please try again later.", requestURI);
            body.setProperty("retryAfter", retryAfterSeconds);
            writeProblem(httpResponse, body);
            return;
        }

        httpResponse.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
        httpResponse.setHeader("X-Rate-Limit-Reset", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
        chain.doFilter(request, response);
    }

    private static ProblemDetail problem(HttpStatus status, String detail, String requestURI) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setInstance(URI.create(requestURI));
        return body;
    }

    // Runs before any controller, so GlobalExceptionHandler never sees these rejections.
    private void writeProblem(HttpServletResponse response, ProblemDetail body) throws IOException {
        response.setStatus(body.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /**
     * Redis key for a logical rate-limit key. Hashing keeps client addresses and attacker-supplied
     * header text out of Redis key names while staying identical on every backend instance.
     */
    static byte[] redisKey(String rateLimitKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rateLimitKey.getBytes(StandardCharsets.UTF_8));
            return (REDIS_KEY_PREFIX + HexFormat.of().formatHex(digest)).getBytes(StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform is required to provide SHA-256.
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the appropriate bucket configuration based on endpoint pattern
     */
    private BucketConfiguration getConfigurationForEndpoint(String path, String method) {
        // Token refresh has its own, larger bucket so that clients behind one address do not
        // exhaust each other's refreshes; every other authentication endpoint keeps the strict one.
        if (path.equals("/api/auth/refresh")) {
            return RateLimitingConfig.REFRESH_CONFIGURATION;
        }

        // Authentication endpoints
        if (path.startsWith("/api/auth/")) {
            return RateLimitingConfig.AUTH_CONFIGURATION;
        }
        
        // Admin endpoints
        if (path.startsWith("/api/v1/admins/") || 
            path.contains("/admin") ||
            (path.startsWith("/api/v1/") && method.equals("DELETE"))) {
            return RateLimitingConfig.ADMIN_CONFIGURATION;
        }
        
        // Upload endpoints
        if (path.contains("/upload") || 
            path.contains("/export") ||
            path.contains("/import") ||
            path.contains("/file")) {
            return RateLimitingConfig.UPLOAD_CONFIGURATION;
        }
        
        // Listing endpoints (GET requests to list resources)
        if (method.equals("GET") && (
            path.startsWith("/api/v1/students") ||
            path.startsWith("/api/v1/teachers") ||
            path.startsWith("/api/v1/classes") ||
            path.startsWith("/api/v1/courses") ||
            path.startsWith("/api/v1/timetables") ||
            path.startsWith("/api/v1/announcements") ||
            path.startsWith("/api/v1/resources") ||
            path.startsWith("/api/v1/grades") ||
            path.startsWith("/api/v1/dashboard"))) {
            return RateLimitingConfig.LISTING_CONFIGURATION;
        }
        
        // Default API bucket for all other endpoints
        return RateLimitingConfig.API_CONFIGURATION;
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
