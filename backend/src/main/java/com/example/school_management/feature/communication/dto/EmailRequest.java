package com.example.school_management.feature.communication.dto;

import com.example.school_management.feature.communication.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String recipientEmail;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    private String recipientName;

    private String senderName;

    private String senderEmail;

    @Builder.Default
    private Boolean isHtml = true;

    @Builder.Default
    private Notification.Priority priority = Notification.Priority.MEDIUM;

    private List<EmailAttachment> attachments;

    private Map<String, Object> templateVariables;

    private String templateName;

    private LocalDateTime scheduledAt;

    private String category;

    private Map<String, String> customHeaders;

    private String replyTo;

    private List<String> ccEmails;

    private List<String> bccEmails;

    private String trackingId;

    @Builder.Default
    private Boolean enableTracking = true;

    @Builder.Default
    private Boolean enableClickTracking = true;

    @Builder.Default
    private Boolean enableOpenTracking = true;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmailAttachment {
        @NotBlank
        private String fileName;
        
        @NotNull
        private byte[] content;
        
        private String contentType;
        
        @Builder.Default
        private Boolean isInline = false;
        
        private String contentId; // For inline attachments
    }
}
