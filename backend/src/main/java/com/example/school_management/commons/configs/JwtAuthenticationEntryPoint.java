package com.example.school_management.commons.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Answers requests rejected by the security filter chain for missing or invalid
 * authentication. This runs before any controller, so GlobalExceptionHandler
 * never sees it and the problem detail is written here. The response never
 * includes the authentication exception's message.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource");
        body.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
