package com.example.school_management.commons.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every HTTP request a correlation ID, returned in the X-Request-ID response header and
 * held in the logging MDC as "requestId" while the request is processed.
 *
 * <p>A caller-supplied X-Request-ID is kept only when it is a short token of letters, digits,
 * '-', '_' and '.'; anything else is replaced, so the header cannot write arbitrary text into log lines.
 * Runs ahead of the security filter chain, so 401/403/429 responses carry the header too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-ID";
    public static final String MDC_KEY = "requestId";

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Error and async dispatches of the same request keep the ID chosen on the first pass.
        String requestId = (String) request.getAttribute(ATTRIBUTE);
        if (requestId == null) {
            requestId = resolve(request.getHeader(HEADER));
            request.setAttribute(ATTRIBUTE, requestId);
            response.setHeader(HEADER, requestId);
        }
        MDC.put(MDC_KEY, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private static String resolve(String incoming) {
        return incoming != null && SAFE_ID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();
    }
}
