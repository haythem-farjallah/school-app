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

import static com.example.school_management.feature.auth.service.CustomUserDetailsService.PASSWORD_CHANGE_REQUIRED;

/**
 * Answers requests rejected by the security filter chain for missing or invalid
 * authentication. This runs before any controller, so GlobalExceptionHandler
 * never sees it and the problem detail is written here. The response never
 * includes the authentication exception's message.
 *
 * A valid token whose account still has to change its password is answered 403
 * with PASSWORD_CHANGE_REQUIRED instead: the credentials are fine, the account
 * just has a pending required action.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        ProblemDetail body = request.getAttribute(PASSWORD_CHANGE_REQUIRED) != null
                ? ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, PASSWORD_CHANGE_REQUIRED)
                : ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource");
        body.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(body.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
