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
public class BulkSMSRequest {

    @NotEmpty(message = "Recipient phones list cannot be empty")
    @Size(max = 1000, message = "Maximum 1000 recipients allowed per bulk SMS request")
    private List<BulkSMSRecipient> recipients;

    @NotBlank(message = "Message content is required")
    @Size(max = 1600, message = "SMS message cannot exceed 1600 characters")
    private String message;

    private String senderName;

    private String senderId;

    @Builder.Default
    private Notification.Priority priority = Notification.Priority.MEDIUM;

    private String templateName;

    private Map<String, Object> globalTemplateVariables;

    private LocalDateTime scheduledAt;

    private String category;

    @Builder.Default
    private String countryCode = "+1";

    @Builder.Default
    private Boolean enableDeliveryReceipt = true;

    @Builder.Default
    private Boolean enableStatusCallback = true;

    private String callbackUrl;

    @Builder.Default
    private SMSRequest.SMSType smsType = SMSRequest.SMSType.TRANSACTIONAL;

    @Builder.Default
    private Integer validityPeriod = 2880; // 48 hours

    @Builder.Default
    private Boolean concatenateMessage = true;

    private Integer maxParts;

    @Builder.Default
    private String encoding = "UTF-8";

    // Bulk-specific settings
    @Builder.Default
    private Integer batchSize = 100; // Process in batches

    @Builder.Default
    private Integer delayBetweenBatches = 1000; // Milliseconds

    @Builder.Default
    private Integer maxRetries = 3;

    @Builder.Default
    private Integer retryDelayMinutes = 5;

    // Cost control
    private Double maxTotalCost;

    private Double maxCostPerMessage;

    // Campaign information
    private String campaignName;

    private String campaignId;

    private String campaignDescription;

    // Compliance settings
    @Builder.Default
    private Boolean respectOptOuts = true;

    @Builder.Default
    private Boolean requireOptIn = false;

    private List<String> excludePhones; // Phones to exclude from bulk send

    // Scheduling options
    private TimeZoneSettings timeZoneSettings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkSMSRecipient {
        @NotBlank(message = "Recipient phone is required")
        private String phone;

        private String name;

        private Map<String, Object> personalizedVariables;

        private List<String> tags;

        private String userId;

        private Notification.RecipientType recipientType;

        private String timeZone;

        private String preferredLanguage;

        @Builder.Default
        private Boolean optedIn = true;

        private LocalDateTime lastOptInDate;

        // Custom scheduling for this recipient
        private LocalDateTime customScheduledAt;

        // Recipient-specific settings
        private String customSenderId;

        private SMSRequest.SMSType customSmsType;

        private Double maxCost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeZoneSettings {
        @Builder.Default
        private Boolean respectRecipientTimeZone = false;

        private String defaultTimeZone;

        private Integer earliestHour; // 0-23

        private Integer latestHour; // 0-23

        private List<Integer> excludedDays; // 0=Sunday, 1=Monday, etc.

        @Builder.Default
        private Boolean skipHolidays = false;

        private List<String> holidayCalendars;
    }

    // Helper methods
    public int getTotalRecipients() {
        return recipients != null ? recipients.size() : 0;
    }

    public List<BulkSMSRecipient> getOptedInRecipients() {
        if (recipients == null) return List.of();
        return recipients.stream()
                .filter(r -> Boolean.TRUE.equals(r.getOptedIn()))
                .toList();
    }

    public int getEstimatedParts() {
        if (message == null) return 0;
        if (message.length() <= 160) return 1;
        return (int) Math.ceil(message.length() / 153.0);
    }

    public boolean requiresUnicodeEncoding() {
        if (message == null) return false;
        return !message.matches("^[\\x00-\\x7F]*$");
    }

    public Double getEstimatedTotalCost() {
        // This would calculate based on recipients, message parts, and provider rates
        // Placeholder implementation
        int parts = getEstimatedParts();
        int recipients = getTotalRecipients();
        double costPerPart = 0.0075; // Example rate
        return parts * recipients * costPerPart;
    }
}
