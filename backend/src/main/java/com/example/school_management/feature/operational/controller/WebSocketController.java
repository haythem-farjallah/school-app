package com.example.school_management.feature.operational.controller;

import com.example.school_management.feature.operational.service.impl.RealTimeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final RealTimeNotificationService notificationService;

    /**
     * Handle user connection to admin feeds
     */
    @SubscribeMapping("/topic/admin-feeds")
    public void subscribeToAdminFeeds(Principal principal) {
        String principalName = principal != null ? principal.getName() : "anonymous";
        log.debug("User {} subscribed to admin feeds", principalName);
        // Could add connection tracking here if needed
    }

    /**
     * Handle user connection to notifications
     */
    @SubscribeMapping("/topic/notifications/{role}")
    public void subscribeToNotifications(@DestinationVariable String role, Principal principal) {
        String principalName = principal != null ? principal.getName() : "anonymous";
        log.debug("User {} subscribed to {} notifications", principalName, role);
        // Could add role validation here if needed
    }

    /**
     * Handle user connection to personal notification queue
     */
    @SubscribeMapping("/queue/user/{userId}/notifications")
    public void subscribeToPersonalNotifications(@DestinationVariable Long userId, Principal principal) {
        String principalName = principal != null ? principal.getName() : "anonymous";
        log.debug("User {} subscribed to personal notifications", principalName);
        
        // Extract user role from authentication (simplified for demo)
        String userRole = extractUserRole(principal);
        
        // Send connection confirmation
        notificationService.sendConnectionConfirmation(userId, userRole);
    }

    /**
     * Handle test message (for debugging)
     */
    @MessageMapping("/test")
    @SendTo("/topic/test")
    public Map<String, Object> handleTestMessage(@Payload Map<String, Object> message, Principal principal) {
        String principalName = principal != null ? principal.getName() : "anonymous";
        log.debug("Received test message from {}: {}", principalName, message);
        
        message.put("response", "Test message received successfully");
        message.put("timestamp", java.time.LocalDateTime.now());
        message.put("from", principalName);
        
        return message;
    }

    /**
     * Handle admin broadcast message
     */
    @MessageMapping("/admin/broadcast")
    public void handleAdminBroadcast(@Payload Map<String, Object> message, Principal principal) {
        log.debug("Admin broadcast from {}: {}", principal.getName(), message);
        
        String title = (String) message.get("title");
        String content = (String) message.get("message");
        String priority = (String) message.getOrDefault("priority", "MEDIUM");
        
        if (title != null && content != null) {
            notificationService.broadcastToAll(title, content, priority);
        }
    }

    /**
     * Handle role-specific broadcast message
     */
    @MessageMapping("/broadcast/roles")
    public void handleRoleBroadcast(@Payload Map<String, Object> message, Principal principal) {
        log.debug("Role broadcast from {}: {}", principal.getName(), message);
        
        String title = (String) message.get("title");
        String content = (String) message.get("message");
        String priority = (String) message.getOrDefault("priority", "MEDIUM");
        
        @SuppressWarnings("unchecked")
        Set<String> targetRoles = (Set<String>) message.get("targetRoles");
        
        if (title != null && content != null && targetRoles != null) {
            notificationService.notifyUserRoles(title, content, priority, targetRoles);
        }
    }

    /**
     * Handle system alert broadcast
     */
    @MessageMapping("/admin/system-alert")
    public void handleSystemAlert(@Payload Map<String, Object> message, Principal principal) {
        log.debug("System alert from {}: {}", principal.getName(), message);
        
        String title = (String) message.get("title");
        String content = (String) message.get("message");
        String priority = (String) message.getOrDefault("priority", "HIGH");
        
        if (title != null && content != null) {
            notificationService.broadcastSystemAlert(title, content, priority);
        }
    }

    /**
     * Handle user-specific message
     */
    @MessageMapping("/message/user")
    public void handleUserMessage(@Payload Map<String, Object> message, Principal principal) {
        log.debug("User message from {}: {}", principal.getName(), message);
        
        String title = (String) message.get("title");
        String content = (String) message.get("message");
        String priority = (String) message.getOrDefault("priority", "MEDIUM");
        
        @SuppressWarnings("unchecked")
        Set<Long> targetUserIds = (Set<Long>) message.get("targetUserIds");
        
        if (title != null && content != null && targetUserIds != null) {
            notificationService.notifySpecificUsers(title, content, priority, targetUserIds);
        }
    }

    /**
     * Echo message for testing connectivity
     */
    @MessageMapping("/echo")
    @SendTo("/topic/echo")
    public Map<String, Object> echo(@Payload Map<String, Object> message, Principal principal) {
        log.debug("Echo message from {}: {}", principal.getName(), message);
        
        message.put("echo", true);
        message.put("timestamp", java.time.LocalDateTime.now());
        message.put("sender", principal.getName());
        
        return message;
    }

    /**
     * Extract user role from authentication (simplified implementation)
     * In a real application, you'd extract this from the user's authorities
     */
    private String extractUserRole(Principal principal) {
        if (principal instanceof Authentication auth) {
            return auth.getAuthorities().stream()
                    .findFirst()
                    .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                    .orElse("USER");
        }
        return principal != null ? "USER" : "ANONYMOUS";
    }
} 