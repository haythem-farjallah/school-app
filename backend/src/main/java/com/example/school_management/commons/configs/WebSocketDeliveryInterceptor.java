package com.example.school_management.commons.configs;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * Delivers an application message to a browser only while the session it is addressed to is still
 * authorized, so a session whose account was suspended, deleted or revoked after CONNECT receives
 * nothing further. Protocol frames (CONNECTED, receipts, heartbeats, disconnect acknowledgements) pass.
 * A suppressed message is dropped rather than refused with an exception, which Spring would log whole.
 */
@Component
@RequiredArgsConstructor
public class WebSocketDeliveryInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketDeliveryInterceptor.class);

    private final WebSocketSessions sessions;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) {
            return message;
        }

        String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
        boolean authorized = sessionId != null && sessions.principal(sessionId)
                .map(sessions::isAuthorized)
                .orElse(false);
        if (!authorized) {
            log.debug("WebSocket outbound message suppressed for revoked session");
            return null;
        }
        return message;
    }
}
