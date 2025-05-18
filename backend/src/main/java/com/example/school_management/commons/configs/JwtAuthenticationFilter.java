package com.example.school_management.commons.configs;

import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider         jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars");
    }

    /* ------------------------------------------------------------ */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);          // null if missing/invalid format

        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                String email = jwtTokenProvider.getEmailFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (ExpiredJwtException ex) {
                log.warn("JWT expired for {}: {}", request.getRequestURI(), ex.getMessage());
            } catch (Exception ex) {
                log.error("JWT processing error: {}", ex.getMessage(), ex);
            }
        }

        chain.doFilter(request, response);
    }

    /* ------------------------------------------------------------ */
    /**
     * Extract raw JWT from the Authorization header.
     * Accepts headers like:
     *   - "Bearer eyJhbGciOiJI..."
     *   - "Bearer   Bearer  eyJhbGciOiJI..."  (defensive trimming)
     */
    private static String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        log.debug("Authorization Header: '{}'", header);

        if (header == null) return null;

        // Remove one or more "Bearer " prefixes (case-insensitive) and trim spaces/CRLF
        String token = header.replaceFirst("(?i)^Bearer\\s+", "").trim();

        // If we stripped nothing, the header didn't start with Bearer
        return token.equals(header) ? null : token.replaceAll("\\s+", "");
    }
}
