//package com.example.school_management.commons.configs;
//
//
//
////import com.project.feature.user.services.imp.CustomUserDetailsService;
//import io.jsonwebtoken.ExpiredJwtException;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//    private final JwtTokenProvider jwtTokenProvider;
//    private final CustomUserDetailsService customUserDetailsService;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws ServletException, IOException {
//        String jwt = getJwtFromRequest(request);
//
//        if (jwt != null) {
//            try {
//                if (jwtTokenProvider.validateToken(jwt)) {
//                    // Get email from JWT
//                    String email = jwtTokenProvider.getEmailFromToken(jwt);
//
//                    // Load user associated with email
//                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
//
//                    // Create authentication token
//                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//                            userDetails, null, userDetails.getAuthorities());
//
//                    // Set details
//                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//
//                    // Set authentication in context
//                    SecurityContextHolder.getContext().setAuthentication(authentication);
//                }
//            } catch (ExpiredJwtException ex) {
//                log.warn("JWT token is expired: {}", ex.getMessage());
//            } catch (Exception ex) {
//                log.error("Could not set user authentication in security context", ex);
//            }
//        }
//
//        // Continue filter chain
//        filterChain.doFilter(request, response);
//    }
//
//    private String getJwtFromRequest(HttpServletRequest request) {
//        String bearerToken = request.getHeader("Authorization");
//        log.debug("Authorization Header: '{}'", bearerToken);
//        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
//            return bearerToken.substring(7).trim();
//        }
//        return null;
//    }
//}