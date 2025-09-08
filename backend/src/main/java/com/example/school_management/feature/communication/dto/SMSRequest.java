package com.example.school_management.feature.communication.dto;

import com.example.school_management.feature.communication.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SMSRequest {

    @NotBlank(message = "Recipient phone number is required")
    private String recipientPhone;

    @NotBlank(message = "Message content is required")
    @Size(max = 1600, message = "SMS message cannot exceed 1600 characters")
    private String message;

    private String recipientName;

    private String senderName;

    private String senderId; // Custom sender ID if supported

    @Builder.Default
    private Notification.Priority priority = Notification.Priority.MEDIUM;

    private Map<String, Object> templateVariables;

    private String templateName;

    private LocalDateTime scheduledAt;

    private String category;

    @Builder.Default
    private String countryCode = "+1"; // Default to US

    private String trackingId;

    @Builder.Default
    private Boolean enableDeliveryReceipt = true;

    @Builder.Default
    private Boolean enableStatusCallback = true;

    private String callbackUrl;

    // SMS-specific settings
    @Builder.Default
    private SMSType smsType = SMSType.TRANSACTIONAL;

    @Builder.Default
    private Integer validityPeriod = 2880; // Minutes (48 hours default)

    private String messageClass; // For flash SMS, etc.

    @Builder.Default
    private Boolean concatenateMessage = true; // For long messages

    private Integer maxParts; // Maximum parts for concatenated SMS

    // Cost control
    private Double maxCost; // Maximum cost willing to pay

    @Builder.Default
    private String encoding = "UTF-8";

    // Retry settings
    @Builder.Default
    private Integer maxRetries = 3;

    @Builder.Default
    private Integer retryDelayMinutes = 5;

    public enum SMSType {
        TRANSACTIONAL,  // OTP, alerts, confirmations
        PROMOTIONAL,    // Marketing messages
        EMERGENCY,      // Emergency alerts
        REMINDER,       // Appointment reminders, due dates
        NOTIFICATION    // General notifications
    }

    // Validation methods
    public boolean isLongMessage() {
        return message != null && message.length() > 160;
    }

    public int getEstimatedParts() {
        if (message == null) return 0;
        if (message.length() <= 160) return 1;
        return (int) Math.ceil(message.length() / 153.0); // 153 chars per part for concatenated SMS
    }

    public boolean requiresUnicodeEncoding() {
        if (message == null) return false;
        return !message.matches("^[\\x00-\\x7F]*$"); // Contains non-ASCII characters
    }
}
