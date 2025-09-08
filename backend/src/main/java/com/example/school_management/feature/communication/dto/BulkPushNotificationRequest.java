package com.example.school_management.feature.communication.dto;

import com.example.school_management.feature.communication.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkPushNotificationRequest {

    @NotEmpty(message = "Recipients list cannot be empty")
    @Size(max = 1000, message = "Maximum 1000 recipients allowed per bulk push notification")
    private List<BulkPushRecipient> recipients;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @NotBlank(message = "Body is required")
    @Size(max = 500, message = "Body cannot exceed 500 characters")
    private String body;

    private String icon;

    private String image;

    private String sound;

    private String clickAction;

    private String category;

    @Builder.Default
    private Notification.Priority priority = Notification.Priority.MEDIUM;

    private Map<String, Object> globalData;

    private String templateName;

    private Map<String, Object> globalTemplateVariables;

    private LocalDateTime scheduledAt;

    @Builder.Default
    private Boolean enableSound = true;

    @Builder.Default
    private Boolean enableVibration = true;

    @Builder.Default
    private Boolean enableLights = true;

    private String color;

    private Integer timeToLive;

    private String collapseKey;

    // Platform-specific settings
    private PushNotificationRequest.AndroidSettings androidSettings;

    private PushNotificationRequest.IOSSettings iosSettings;

    private PushNotificationRequest.WebSettings webSettings;

    // Bulk-specific settings
    @Builder.Default
    private Integer batchSize = 100;

    @Builder.Default
    private Integer delayBetweenBatches = 500; // Milliseconds

    @Builder.Default
    private Integer maxRetries = 3;

    @Builder.Default
    private Integer retryDelayMinutes = 5;

    // Campaign information
    private String campaignName;

    private String campaignId;

    private String campaignDescription;

    // Targeting options
    private List<String> targetRoles;

    private List<Long> targetClasses;

    private List<String> targetTags;

    private String targetCondition;

    // Delivery options
    @Builder.Default
    private Boolean respectUserPreferences = true;

    @Builder.Default
    private Boolean respectQuietHours = true;

    private QuietHoursSettings quietHoursSettings;

    // Analytics and tracking
    private String trackingId;

    @Builder.Default
    private Boolean enableAnalytics = true;

    @Builder.Default
    private Boolean enableClickTracking = true;

    @Builder.Default
    private Boolean enableOpenTracking = true;

    // Fallback options
    @Builder.Default
    private Boolean enableWebSocketFallback = true;

    @Builder.Default
    private Boolean enableEmailFallback = false;

    @Builder.Default
    private Boolean enableSMSFallback = false;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkPushRecipient {
        @NotBlank(message = "Recipient ID is required")
        private String recipientId;

        private String name;

        private List<String> deviceTokens;

        private String platform; // "android", "ios", "web"

        private Map<String, Object> personalizedData;

        private Map<String, Object> personalizedVariables;

        private List<String> tags;

        private Notification.RecipientType recipientType;

        private String timeZone;

        private String preferredLanguage;

        // Recipient-specific settings
        private String customTitle;

        private String customBody;

        private String customIcon;

        private String customImage;

        private String customSound;

        private String customClickAction;

        private LocalDateTime customScheduledAt;

        private Notification.Priority customPriority;

        // Preferences
        @Builder.Default
        private Boolean pushNotificationsEnabled = true;

        @Builder.Default
        private Boolean soundEnabled = true;

        @Builder.Default
        private Boolean vibrationEnabled = true;

        private List<String> mutedCategories;

        private QuietHoursSettings personalQuietHours;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuietHoursSettings {
        @Builder.Default
        private Boolean enabled = false;

        private Integer startHour; // 0-23

        private Integer endHour; // 0-23

        private List<Integer> excludedDays; // 0=Sunday, 1=Monday, etc.

        private String timeZone;

        @Builder.Default
        private Boolean allowUrgent = true; // Allow urgent notifications during quiet hours

        private List<String> allowedCategories; // Categories allowed during quiet hours
    }

    // Helper methods
    public int getTotalRecipients() {
        return recipients != null ? recipients.size() : 0;
    }

    public List<BulkPushRecipient> getEnabledRecipients() {
        if (recipients == null) return List.of();
        return recipients.stream()
                .filter(r -> Boolean.TRUE.equals(r.getPushNotificationsEnabled()))
                .toList();
    }

    public List<BulkPushRecipient> getRecipientsByPlatform(String platform) {
        if (recipients == null) return List.of();
        return recipients.stream()
                .filter(r -> platform.equals(r.getPlatform()))
                .toList();
    }

    public List<BulkPushRecipient> getRecipientsByRole(Notification.RecipientType role) {
        if (recipients == null) return List.of();
        return recipients.stream()
                .filter(r -> role.equals(r.getRecipientType()))
                .toList();
    }

    public boolean isScheduled() {
        return scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now());
    }

    public boolean isHighPriority() {
        return Notification.Priority.HIGH.equals(priority) || 
               Notification.Priority.URGENT.equals(priority);
    }

    public boolean hasGlobalData() {
        return globalData != null && !globalData.isEmpty();
    }

    public boolean isTargetingRoles() {
        return targetRoles != null && !targetRoles.isEmpty();
    }

    public boolean isTargetingClasses() {
        return targetClasses != null && !targetClasses.isEmpty();
    }

    public boolean isTargetingTags() {
        return targetTags != null && !targetTags.isEmpty();
    }

    public boolean hasFallbackOptions() {
        return enableWebSocketFallback || enableEmailFallback || enableSMSFallback;
    }

    public int getEstimatedBatches() {
        int totalRecipients = getTotalRecipients();
        return (int) Math.ceil((double) totalRecipients / batchSize);
    }

    public long getEstimatedDeliveryTime() {
        int batches = getEstimatedBatches();
        return (long) batches * delayBetweenBatches; // In milliseconds
    }
}
