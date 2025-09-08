package com.example.school_management.feature.communication.dto;

import com.example.school_management.feature.communication.entity.NotificationTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String templateName;

    @NotNull(message = "Template type is required")
    private NotificationTemplate.TemplateType templateType;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    private List<String> variables;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private String language = "en";

    private String category;

    private String description;

    // Additional metadata for template management
    private String version;

    private String author;

    private List<String> tags;

    private String previewText;

    // Template-specific settings
    private TemplateSettings settings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateSettings {
        @Builder.Default
        private Boolean enableTracking = true;

        @Builder.Default
        private Boolean enableClickTracking = true;

        @Builder.Default
        private Boolean enableOpenTracking = true;

        private String unsubscribeUrl;

        private String footerText;

        private String headerImageUrl;

        private String brandColor;

        private String fontFamily;

        @Builder.Default
        private Integer maxRetries = 3;

        @Builder.Default
        private Integer retryDelayMinutes = 5;

        private List<String> allowedRecipientTypes;

        private String cssStyles;

        @Builder.Default
        private Boolean requiresApproval = false;

        private String approvalWorkflow;
    }
}
