package com.example.school_management.feature.communication.dto;

import com.example.school_management.feature.communication.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushNotificationRequest {

    private String recipientId; // User ID

    private List<String> recipientIds; // Multiple user IDs

    private String recipientType; // "user", "role", "class", "all"

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @NotBlank(message = "Body is required")
    @Size(max = 500, message = "Body cannot exceed 500 characters")
    private String body;

    private String icon;

    private String image;

    private String sound;

    private String badge;

    private String clickAction;

    private String category;

    @Builder.Default
    private Notification.Priority priority = Notification.Priority.MEDIUM;

    private Map<String, Object> data; // Custom data payload

    private Map<String, Object> templateVariables;

    private String templateName;

    private LocalDateTime scheduledAt;

    @Builder.Default
    private Boolean enableSound = true;

    @Builder.Default
    private Boolean enableVibration = true;

    @Builder.Default
    private Boolean enableLights = true;

    private String color; // LED color for Android

    private Integer timeToLive; // TTL in seconds

    @Builder.Default
    private String collapseKey = "default";

    @Builder.Default
    private Boolean contentAvailable = false; // For iOS background updates

    @Builder.Default
    private Boolean mutableContent = false; // For iOS notification extensions

    // Platform-specific settings
    private AndroidSettings androidSettings;

    private IOSSettings iosSettings;

    private WebSettings webSettings;

    // Targeting options
    private List<String> tags; // User tags for targeting

    private List<String> topics; // FCM topics

    private String condition; // FCM condition

    // Analytics and tracking
    private String trackingId;

    private String campaignId;

    private String campaignName;

    @Builder.Default
    private Boolean enableAnalytics = true;

    // Action buttons
    private List<NotificationAction> actions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AndroidSettings {
        private String channelId;

        private String smallIcon;

        private String largeIcon;

        private String color;

        @Builder.Default
        private Integer priority = 0; // -2 to 2

        @Builder.Default
        private String visibility = "private"; // private, public, secret

        private List<String> vibratePattern;

        private List<Integer> lightSettings; // [color, onMs, offMs]

        @Builder.Default
        private Boolean autoCancel = true;

        @Builder.Default
        private Boolean ongoing = false;

        private String group;

        private String groupSummary;

        private String ticker;

        private Map<String, String> customData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IOSSettings {
        private String sound;

        private Integer badge;

        @Builder.Default
        private Boolean contentAvailable = false;

        @Builder.Default
        private Boolean mutableContent = false;

        private String category;

        private String threadId;

        private String targetContentId;

        private Double interruptionLevel; // 0.0 to 1.0

        private String relevanceScore;

        private Map<String, Object> customData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WebSettings {
        private String icon;

        private String image;

        private String badge;

        private String tag;

        @Builder.Default
        private Boolean requireInteraction = false;

        @Builder.Default
        private Boolean silent = false;

        private Integer[] vibrate;

        private String dir; // ltr, rtl, auto

        private String lang;

        private List<NotificationAction> actions;

        private Map<String, Object> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationAction {
        @NotBlank
        private String action;

        @NotBlank
        private String title;

        private String icon;

        private String type; // button, input

        private String placeholder; // For input type

        private Map<String, Object> data;
    }

    // Helper methods
    public boolean isMulticast() {
        return recipientIds != null && recipientIds.size() > 1;
    }

    public boolean isBroadcast() {
        return "all".equals(recipientType);
    }

    public boolean isRoleTargeted() {
        return "role".equals(recipientType);
    }

    public boolean isClassTargeted() {
        return "class".equals(recipientType);
    }

    public boolean hasCustomData() {
        return data != null && !data.isEmpty();
    }

    public boolean hasActions() {
        return actions != null && !actions.isEmpty();
    }

    public boolean isScheduled() {
        return scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now());
    }

    public boolean isHighPriority() {
        return Notification.Priority.HIGH.equals(priority) || 
               Notification.Priority.URGENT.equals(priority);
    }
}
