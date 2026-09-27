package com.example.school_management.commons.configs;

import com.example.school_management.commons.configs.JwtTokenProvider.ValidatedToken;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

import static com.example.school_management.feature.auth.service.CustomUserDetailsService.PASSWORD_CHANGE_REQUIRED;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final RequestMatcher CHANGE_PASSWORD =
            AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/auth/change-password");

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

        // Only access tokens authenticate requests; a refresh token is only accepted by /api/auth/refresh.
        Optional<ValidatedToken> validated =
                token == null ? Optional.empty() : jwtTokenProvider.validateAccessToken(token);

        if (validated.isPresent()) {
            try {
                BaseUser user = userDetailsService.findBaseUserByEmail(validated.get().email());

                // A token issued before the account's sessions were revoked is treated as invalid.
                if (validated.get().tokenVersion() != user.getTokenVersion()) {
                    log.debug("JWT token version is no longer current");
                    chain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails = userDetailsService.toUserDetails(user);

                if (!userDetails.isEnabled()) {
                    log.debug("JWT belongs to a disabled account");
                    chain.doFilter(request, response);
                    return;
                }

                // Until the required password change is done, the token only authenticates that change.
                // Other requests stay anonymous, so public endpoints still work and protected ones are
                // answered 403 by the entry point.
                if (requiresPasswordChange(userDetails) && !CHANGE_PASSWORD.matches(request)) {
                    request.setAttribute(PASSWORD_CHANGE_REQUIRED, Boolean.TRUE);
                    chain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception ex) {
                log.error("JWT processing error: {}", ex.getMessage(), ex);
            }
        }

        chain.doFilter(request, response);
    }

    private static boolean requiresPasswordChange(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> PASSWORD_CHANGE_REQUIRED.equals(a.getAuthority()));
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

        if (header == null) return null;

        // Remove one or more "Bearer " prefixes (case-insensitive) and trim spaces/CRLF
        String token = header.replaceFirst("(?i)^Bearer\\s+", "").trim();

        // If we stripped nothing, the header didn't start with Bearer
        return token.equals(header) ? null : token.replaceAll("\\s+", "");
    }
}
