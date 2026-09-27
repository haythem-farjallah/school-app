package com.example.school_management.commons.configs;

import com.example.school_management.commons.configs.JwtTokenProvider.ValidatedToken;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Secures the STOMP frames that arrive over /ws and /ws-native. The HTTP handshake is public, so a
 * session is authenticated by its CONNECT frame, which must carry a current ACCESS token of an account
 * that may use the API, exactly as JwtAuthenticationFilter requires for HTTP. Every later frame runs
 * as that account, and a SUBSCRIBE is accepted only for the destinations the account may read.
 * WebSocket delivery is server to browser only, so every client SEND is refused.
 * A refused frame is answered with a generic STOMP ERROR, after which Spring closes the session.
 */
@Component
@RequiredArgsConstructor
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSecurityInterceptor.class);

    static final String AUTHENTICATION_FAILED = "WebSocket authentication failed";
    static final String ACCESS_DENIED = "WebSocket access denied";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    // Every authenticated account receives system alerts; the audience is revisited with notifications.
    private static final String SYSTEM_ALERTS = "/topic/system-alerts";
    private static final String ADMIN_FEEDS = "/topic/admin-feeds";

    private final JwtTokenProvider         jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;   // heartbeats
        }

        switch (accessor.getCommand()) {
            case CONNECT, STOMP -> accessor.setUser(authenticate(accessor));
            // Also sent by Spring itself when a session closes, authenticated or not.
            case DISCONNECT -> { }
            case SUBSCRIBE -> authorizeSubscription(accessor);
            case SEND -> {
                authenticatedUser(accessor);
                throw rejected("WebSocket send denied", ACCESS_DENIED);
            }
            default -> authenticatedUser(accessor);
        }
        return message;
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String token = bearerToken(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION));

        // Only access tokens authenticate; a refresh token is only accepted by /api/auth/refresh.
        Optional<ValidatedToken> validated =
                token == null ? Optional.empty() : jwtTokenProvider.validateAccessToken(token);
        if (validated.isEmpty()) {
            throw rejected("WebSocket authentication rejected", AUTHENTICATION_FAILED);
        }

        BaseUser user;
        try {
            user = userDetailsService.findBaseUserByEmail(validated.get().email());
        } catch (UsernameNotFoundException ex) {
            throw rejected("WebSocket authentication rejected", AUTHENTICATION_FAILED);
        }

        UserDetails userDetails = userDetailsService.toUserDetails(user);

        // A token issued before the account's sessions were revoked, a disabled account, and an account
        // that still has to change its password are refused alike.
        if (validated.get().tokenVersion() != user.getTokenVersion()
                || !userDetails.isEnabled()
                || user.isPasswordChangeRequired()) {
            throw rejected("WebSocket authentication rejected", AUTHENTICATION_FAILED);
        }

        log.debug("WebSocket connected");
        return UsernamePasswordAuthenticationToken.authenticated(
                new WebSocketPrincipal(user.getId(), user.getEmail()), null, userDetails.getAuthorities());
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Authentication authentication = authenticatedUser(accessor);
        String destination = accessor.getDestination();
        if (destination == null || !maySubscribe(authentication, destination)) {
            throw rejected("WebSocket subscription denied", ACCESS_DENIED);
        }
    }

    private static boolean maySubscribe(Authentication authentication, String destination) {
        WebSocketPrincipal principal = (WebSocketPrincipal) authentication.getPrincipal();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .toList();

        if (SYSTEM_ALERTS.equals(destination)) {
            return true;
        }
        if (ADMIN_FEEDS.equals(destination)) {
            return roles.contains("ADMIN");
        }
        if (destination.equals("/queue/user/" + principal.accountId() + "/notifications")) {
            return true;
        }
        return roles.stream().anyMatch(role ->
                destination.equals("/topic/notifications/" + role.toLowerCase(Locale.ROOT)));
    }

    /** The account the session authenticated as on CONNECT; refuses the frame if there is none. */
    private static Authentication authenticatedUser(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof Authentication authentication
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof WebSocketPrincipal) {
            return authentication;
        }
        throw rejected("WebSocket frame without authentication rejected", AUTHENTICATION_FAILED);
    }

    private static String bearerToken(String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    // The client only ever sees the generic reason; no token, account or destination is logged.
    private static MessagingException rejected(String logMessage, String clientReason) {
        log.debug(logMessage);
        return new MessagingException(clientReason);
    }
}
