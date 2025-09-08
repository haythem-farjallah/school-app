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
public class BulkEmailRequest {

    @NotEmpty(message = "Recipient emails list cannot be empty")
    @Size(max = 1000, message = "Maximum 1000 recipients allowed per bulk request")
    private List<BulkEmailRecipient> recipients;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    private String senderName;

    private String senderEmail;

    @Builder.Default
    private Boolean isHtml = true;

    @Builder.Default
    private Notification.Priority priority = Notification.Priority.MEDIUM;

    private List<EmailRequest.EmailAttachment> attachments;

    private String templateName;

    private Map<String, Object> globalTemplateVariables;

    private LocalDateTime scheduledAt;

    private String category;

    private Map<String, String> customHeaders;

    private String replyTo;

    @Builder.Default
    private Boolean enableTracking = true;

    @Builder.Default
    private Boolean enableClickTracking = true;

    @Builder.Default
    private Boolean enableOpenTracking = true;

    @Builder.Default
    private Integer batchSize = 50; // Process in batches to avoid overwhelming the system

    @Builder.Default
    private Integer delayBetweenBatches = 1000; // Milliseconds delay between batches

    private String campaignName;

    private String campaignId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkEmailRecipient {
        @NotBlank(message = "Recipient email is required")
        private String email;
        
        private String name;
        
        private Map<String, Object> personalizedVariables; // For template personalization
        
        private List<String> tags; // For segmentation
        
        private String userId; // Link to user in system
        
        private Notification.RecipientType recipientType;
    }
}
