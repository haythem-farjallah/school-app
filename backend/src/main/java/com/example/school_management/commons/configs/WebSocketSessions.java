package com.example.school_management.commons.configs;

import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The account each open STOMP session authenticated as, and whether that session is still authorized
 * by the current state of its account. Spring keeps the per-session principal private, and a message
 * on its way to the browser only names its session, so outbound delivery looks the account up here.
 * An entry lives from the accepted CONNECT until the DISCONNECT Spring sends when the session ends.
 */
@Component
@RequiredArgsConstructor
public class WebSocketSessions {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessions.class);

    private final UserRepository userRepository;

    private final Map<String, WebSocketPrincipal> principals = new ConcurrentHashMap<>();

    void connected(String sessionId, WebSocketPrincipal principal) {
        principals.put(sessionId, principal);
    }

    void closed(String sessionId) {
        principals.remove(sessionId);
    }

    Optional<WebSocketPrincipal> principal(String sessionId) {
        return Optional.ofNullable(principals.get(sessionId));
    }

    /**
     * A session stays authorized only while its account exists, is ACTIVE, has not had its sessions
     * revoked since CONNECT and does not have to change its password. DELETED accounts are not found.
     * If the account cannot be checked, the session is treated as unauthorized.
     */
    boolean isAuthorized(WebSocketPrincipal principal) {
        try {
            return userRepository.findById(principal.accountId())
                    .filter(user -> user.getStatus() == Status.ACTIVE
                            && user.getTokenVersion() == principal.tokenVersion()
                            && !user.isPasswordChangeRequired())
                    .isPresent();
        } catch (RuntimeException ex) {
            log.warn("WebSocket session validation failed: {}", ex.getClass().getSimpleName());
            return false;
        }
    }
}
